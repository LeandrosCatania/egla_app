import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

export default defineSchema({
  locationRecords: defineTable({
    timestamp: v.number(),
    latitude: v.number(), // Enhanced latitude
    longitude: v.number(), // Enhanced longitude
    originalLatitude: v.number(), // Original GPS latitude
    originalLongitude: v.number(), // Original GPS longitude
    accuracy: v.number(), // Enhanced accuracy
    originalAccuracy: v.number(), // Original GPS accuracy
    altitude: v.number(),
    bearing: v.number(),
    speed: v.number(),
    direction: v.string(),
    isStationary: v.boolean(),
    environment: v.string(),
    operatingMode: v.string(),
    accuracyImprovement: v.number(),
    confidence: v.number(),
    processingTime: v.number(),
    deviceId: v.optional(v.string()), // To identify which device sent the data
    sessionId: v.optional(v.string()), // To group records by tracking session
  })
  .index("by_timestamp", ["timestamp"])
  .index("by_device", ["deviceId"])
  .index("by_session", ["sessionId"])
  .index("by_device_timestamp", ["deviceId", "timestamp"]),
}); 