db.getSiblingDB("tutelar");
db.createCollection("users");
db.users.createIndex({"id": 1}, {"unique": true});
db.users.createIndex({"accounts.authType": 1, "accounts.externalId": 1}, {"unique": true});
