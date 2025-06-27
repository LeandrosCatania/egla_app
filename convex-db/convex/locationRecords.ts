import { mutation, query } from "./_generated/server";
import { v } from "convex/values";

// Insert a new location record
export const insertLocationRecord = mutation({
  args: {
    timestamp: v.number(),
    latitude: v.number(),
    longitude: v.number(),
    originalLatitude: v.number(),
    originalLongitude: v.number(),
    accuracy: v.number(),
    originalAccuracy: v.number(),
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
    deviceId: v.optional(v.string()),
    sessionId: v.optional(v.string()),
  },
  handler: async (ctx, args) => {
    const recordId = await ctx.db.insert("locationRecords", args);
    return recordId;
  },
});

// Get all location records for a device
export const getLocationRecordsByDevice = query({
  args: { deviceId: v.string() },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("locationRecords")
      .withIndex("by_device", (q) => q.eq("deviceId", args.deviceId))
      .order("desc")
      .collect();
  },
});

// Get location records by session
export const getLocationRecordsBySession = query({
  args: { sessionId: v.string() },
  handler: async (ctx, args) => {
    return await ctx.db
      .query("locationRecords")
      .withIndex("by_session", (q) => q.eq("sessionId", args.sessionId))
      .order("desc")
      .collect();
  },
});

// Get recent location records (last N records)
export const getRecentLocationRecords = query({
  args: { limit: v.optional(v.number()) },
  handler: async (ctx, args) => {
    const limit = args.limit ?? 100;
    return await ctx.db
      .query("locationRecords")
      .withIndex("by_timestamp")
      .order("desc")
      .take(limit);
  },
});

// Get location records within a time range
export const getLocationRecordsByTimeRange = query({
  args: {
    startTime: v.number(),
    endTime: v.number(),
    deviceId: v.optional(v.string()),
  },
  handler: async (ctx, args) => {
    let query = ctx.db.query("locationRecords");
    
    if (args.deviceId) {
      query = query.withIndex("by_device_timestamp", (q) => 
        q.eq("deviceId", args.deviceId)
         .gte("timestamp", args.startTime)
         .lte("timestamp", args.endTime)
      );
    } else {
      query = query.withIndex("by_timestamp", (q) => 
        q.gte("timestamp", args.startTime)
         .lte("timestamp", args.endTime)
      );
    }
    
    return await query.collect();
  },
});

// Get statistics for a device
export const getDeviceStats = query({
  args: { deviceId: v.string() },
  handler: async (ctx, args) => {
    const records = await ctx.db
      .query("locationRecords")
      .withIndex("by_device", (q) => q.eq("deviceId", args.deviceId))
      .collect();
    
    if (records.length === 0) {
      return {
        totalRecords: 0,
        averageAccuracy: 0,
        averageAccuracyImprovement: 0,
        averageConfidence: 0,
        firstRecord: null,
        lastRecord: null,
      };
    }
    
    const totalAccuracy = records.reduce((sum, record) => sum + record.accuracy, 0);
    const totalImprovement = records.reduce((sum, record) => sum + record.accuracyImprovement, 0);
    const totalConfidence = records.reduce((sum, record) => sum + record.confidence, 0);
    
    const sortedByTime = records.sort((a, b) => a.timestamp - b.timestamp);
    
    return {
      totalRecords: records.length,
      averageAccuracy: totalAccuracy / records.length,
      averageAccuracyImprovement: totalImprovement / records.length,
      averageConfidence: totalConfidence / records.length,
      firstRecord: sortedByTime[0],
      lastRecord: sortedByTime[sortedByTime.length - 1],
    };
  },
});

// Delete old records (cleanup function)
export const deleteOldRecords = mutation({
  args: { 
    olderThanTimestamp: v.number(),
    deviceId: v.optional(v.string()),
  },
  handler: async (ctx, args) => {
    let query = ctx.db.query("locationRecords");
    
    if (args.deviceId) {
      query = query.withIndex("by_device_timestamp", (q) => 
        q.eq("deviceId", args.deviceId)
         .lt("timestamp", args.olderThanTimestamp)
      );
    } else {
      query = query.withIndex("by_timestamp", (q) => 
        q.lt("timestamp", args.olderThanTimestamp)
      );
    }
    
    const recordsToDelete = await query.collect();
    
    for (const record of recordsToDelete) {
      await ctx.db.delete(record._id);
    }
    
    return { deletedCount: recordsToDelete.length };
  },
}); 