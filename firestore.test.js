const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  await testEnv.cleanup();
});

test("unauthenticated user cannot read or write anything", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.doc("users/user_123/routers/router_1").get());
  await assertFails(unauthedDb.doc("users/user_123/routers/router_1").set({
    userId: "user_123",
    name: "Router 1",
    ip: "192.168.88.1",
    port: 80,
    username: "admin",
    createdAt: new Date()
  }));
});

test("authenticated user can create router under their own path", async () => {
  const authedDb = testEnv.authenticatedContext("user_123").firestore();
  await assertSucceeds(authedDb.doc("users/user_123/routers/router_1").set({
    userId: "user_123",
    name: "Main Gateway",
    ip: "192.168.88.1",
    port: 80,
    username: "admin",
    createdAt: new Date()
  }));
});

test("authenticated user cannot access another user's routers", async () => {
  const attackerDb = testEnv.authenticatedContext("attacker_999").firestore();
  await assertFails(attackerDb.doc("users/user_123/routers/router_1").get());
  await assertFails(attackerDb.doc("users/user_123/routers/router_1").set({
    userId: "user_123",
    name: "Hacked",
    ip: "1.1.1.1",
    port: 80,
    username: "admin",
    createdAt: new Date()
  }));
});

test("authenticated user can create network_user with valid schema", async () => {
  const authedDb = testEnv.authenticatedContext("user_123").firestore();
  await assertSucceeds(authedDb.doc("users/user_123/network_users/net_1").set({
    userId: "user_123",
    name: "Ahmed",
    ipAddress: "192.168.88.101",
    quotaGb: 50.0,
    status: "ACTIVE",
    createdAt: new Date()
  }));
});

test("network_user creation fails if invalid status", async () => {
  const authedDb = testEnv.authenticatedContext("user_123").firestore();
  await assertFails(authedDb.doc("users/user_123/network_users/net_1").set({
    userId: "user_123",
    name: "Ahmed",
    ipAddress: "192.168.88.101",
    quotaGb: 50.0,
    status: "INVALID_STATUS",
    createdAt: new Date()
  }));
});
