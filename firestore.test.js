const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
let testEnv;

before(async () => {
  const rules = fs.readFileSync("firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: rules,
      host: "127.0.0.1",
      port: 8085
    }
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("unauthenticated user cannot read or write projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const docRef = unauthDb.doc("users/alice/projects/p1");
  await assertFails(docRef.get());
  await assertFails(docRef.set({
    userId: "alice",
    name: "Sample",
    aspectRatio: "16:9",
    width: 1920,
    height: 1080,
    fps: 30,
    durationMs: 5000,
    backgroundColor: "#0A0F1E",
    layersJson: "[]",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("authenticated user can create and read their own project", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  const docRef = aliceDb.doc("users/alice/projects/p1");
  await assertSucceeds(docRef.set({
    userId: "alice",
    name: "My Motion",
    aspectRatio: "16:9",
    width: 1920,
    height: 1080,
    fps: 30,
    durationMs: 5000,
    backgroundColor: "#0A0F1E",
    layersJson: "[]",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
  await assertSucceeds(docRef.get());
});

test("user cannot read or write another user's project", async () => {
  const bobDb = testEnv.authenticatedContext("bob").firestore();
  const aliceDocRef = bobDb.doc("users/alice/projects/p1");
  await assertFails(aliceDocRef.get());
  await assertFails(aliceDocRef.set({
    userId: "alice",
    name: "Hacked",
    aspectRatio: "16:9",
    width: 1920,
    height: 1080,
    fps: 30,
    durationMs: 5000,
    backgroundColor: "#0A0F1E",
    layersJson: "[]",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});
