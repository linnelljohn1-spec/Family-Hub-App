/**
 * Family Hub push notifications.
 *
 * Each trigger watches a Firestore collection under families/{code} and pushes to the
 * devices registered in families/{code}/devices/{fcmToken} (written by the app's
 * PushTokenService). Device docs carry the memberFirestoreId of whoever uses that phone,
 * which is how recipients are targeted/excluded.
 */
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const logger = require("firebase-functions/logger");

initializeApp();
const db = getFirestore();

// Must match the Firestore database location (europe-west2, London) - Firestore
// triggers can't be deployed to a different region from the database.
setGlobalOptions({ region: "europe-west2", maxInstances: 5 });

// Ignore docs created long before the trigger ran (e.g. bulk migration/backfill).
const MAX_EVENT_AGE_MS = 10 * 60 * 1000;

const INVALID_TOKEN_CODES = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

/**
 * @param {string} code family sync group code
 * @param {{onlyMember?: string, excludeMember?: string|null}} target
 * @param {{type: string, channelId: string, title: string, body: string, id: string}} payload
 */
async function sendToFamily(code, target, payload) {
  const devicesSnap = await db.collection("families").doc(code).collection("devices").get();

  const tokens = devicesSnap.docs
    .filter((doc) => {
      const member = doc.get("memberFirestoreId");
      if (target.onlyMember) return member === target.onlyMember;
      if (target.excludeMember) return member !== target.excludeMember;
      return true;
    })
    .map((doc) => doc.id);

  if (tokens.length === 0) {
    logger.info(`No recipients for ${payload.type} in ${code}`);
    return;
  }

  const response = await getMessaging().sendEachForMulticast({
    tokens,
    notification: { title: payload.title, body: payload.body },
    data: {
      type: payload.type,
      id: payload.id,
      title: payload.title,
      body: payload.body,
    },
    android: {
      priority: "high",
      notification: {
        channelId: payload.channelId,
        // Collapse repeats of the same event on the device.
        tag: `${payload.type}_${payload.id}`,
      },
    },
  });

  // Prune tokens from uninstalled apps / rotated tokens.
  const stale = [];
  response.responses.forEach((res, i) => {
    if (!res.success && res.error && INVALID_TOKEN_CODES.has(res.error.code)) {
      stale.push(db.collection("families").doc(code).collection("devices").doc(tokens[i]).delete());
    }
  });
  await Promise.all(stale);

  logger.info(
    `${payload.type} in ${code}: sent ${response.successCount}/${tokens.length}, pruned ${stale.length}`
  );
}

function isStale(createdAtMs) {
  return typeof createdAtMs === "number" && Date.now() - createdAtMs > MAX_EVENT_AGE_MS;
}

function truncate(text, max = 180) {
  if (!text) return "";
  return text.length > max ? `${text.slice(0, max - 1)}…` : text;
}

exports.onChatMessageCreated = onDocumentCreated("families/{code}/messages/{id}", async (event) => {
  const data = event.data?.data();
  if (!data || isStale(data.timestamp)) return;

  await sendToFamily(
    event.params.code,
    { excludeMember: data.senderMemberFirestoreId ?? null },
    {
      type: "chat",
      channelId: "chat_messages",
      title: data.senderName || "Family Chat",
      body: truncate(data.text),
      id: event.params.id,
    }
  );
});

exports.onPollCreated = onDocumentCreated("families/{code}/polls/{id}", async (event) => {
  const data = event.data?.data();
  if (!data || isStale(data.createdAt)) return;

  await sendToFamily(
    event.params.code,
    { excludeMember: data.createdByMemberFirestoreId ?? null },
    {
      type: "poll",
      channelId: "new_polls",
      title: `New poll from ${data.createdByName || "a family member"}`,
      body: truncate(data.question),
      id: event.params.id,
    }
  );
});

exports.onGoalReached = onDocumentCreated("families/{code}/goalEvents/{id}", async (event) => {
  const data = event.data?.data();
  if (!data || isStale(data.timestamp)) return;

  await sendToFamily(
    event.params.code,
    { excludeMember: data.memberFirestoreId ?? null },
    {
      type: "goal",
      channelId: "goal_reached",
      title: "Goal reached!",
      body: `${data.memberName || "A family member"} just fully funded "${data.goalTitle || "a goal"}"`,
      id: event.params.id,
    }
  );
});

exports.onTaskCreated = onDocumentCreated("families/{code}/tasks/{id}", async (event) => {
  const data = event.data?.data();
  if (!data || isStale(data.createdAt)) return;

  const assignee = data.assignedMemberFirestoreId || null;
  const creator = data.createdByMemberFirestoreId || null;
  // Don't tell people about tasks they assigned to themselves.
  if (assignee && assignee === creator) return;

  const dueLabel = data.dueDate ? ` (due ${data.dueDate})` : "";
  await sendToFamily(
    event.params.code,
    assignee ? { onlyMember: assignee } : { excludeMember: creator },
    {
      type: "task",
      channelId: "new_tasks",
      title: assignee ? "New task for you" : "New family task",
      body: truncate(`${data.title || "Untitled task"}${dueLabel}`),
      id: event.params.id,
    }
  );
});

exports.onWalletDeposit = onDocumentCreated("families/{code}/walletEvents/{id}", async (event) => {
  const data = event.data?.data();
  if (!data || isStale(data.timestamp)) return;

  const recipient = data.memberFirestoreId;
  if (!recipient || recipient === data.byMemberFirestoreId) return;

  const amountLabel = data.amountLabel || String(data.amount);
  const from = data.byMemberName ? ` from ${data.byMemberName}` : "";
  await sendToFamily(
    event.params.code,
    { onlyMember: recipient },
    {
      type: "wallet",
      channelId: "wallet",
      title: "Money added to your wallet",
      body: `You received ${amountLabel}${from}`,
      id: event.params.id,
    }
  );
});
