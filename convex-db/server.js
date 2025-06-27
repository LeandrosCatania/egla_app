const express = require('express');
const { ConvexHttpClient } = require('convex/browser');
const cors = require('cors');

const app = express();
const port = 3000;

// Initialize Convex client
const convex = new ConvexHttpClient(process.env.CONVEX_URL || 'http://localhost:3210');

// Middleware
app.use(cors());
app.use(express.json());

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

// Insert location record
app.post('/api/location', async (req, res) => {
  try {
    const locationData = req.body;
    
    // Validate required fields
    const requiredFields = [
      'timestamp', 'latitude', 'longitude', 'originalLatitude', 'originalLongitude',
      'accuracy', 'originalAccuracy', 'altitude', 'bearing', 'speed', 'direction',
      'isStationary', 'environment', 'operatingMode', 'accuracyImprovement',
      'confidence', 'processingTime'
    ];
    
    for (const field of requiredFields) {
      if (locationData[field] === undefined) {
        return res.status(400).json({ error: `Missing required field: ${field}` });
      }
    }
    
    // Insert the record
    const recordId = await convex.mutation('locationRecords:insertLocationRecord', locationData);
    
    res.json({ 
      success: true, 
      recordId: recordId,
      message: 'Location record saved successfully' 
    });
    
  } catch (error) {
    console.error('Error inserting location record:', error);
    res.status(500).json({ 
      error: 'Failed to save location record',
      details: error.message 
    });
  }
});

// Get recent location records
app.get('/api/location/recent', async (req, res) => {
  try {
    const limit = parseInt(req.query.limit) || 100;
    const records = await convex.query('locationRecords:getRecentLocationRecords', { limit });
    
    res.json({
      success: true,
      records: records,
      count: records.length
    });
    
  } catch (error) {
    console.error('Error fetching recent records:', error);
    res.status(500).json({ 
      error: 'Failed to fetch location records',
      details: error.message 
    });
  }
});

// Get location records by device
app.get('/api/location/device/:deviceId', async (req, res) => {
  try {
    const deviceId = req.params.deviceId;
    const records = await convex.query('locationRecords:getLocationRecordsByDevice', { deviceId });
    
    res.json({
      success: true,
      records: records,
      count: records.length
    });
    
  } catch (error) {
    console.error('Error fetching device records:', error);
    res.status(500).json({ 
      error: 'Failed to fetch device records',
      details: error.message 
    });
  }
});

// Get location records by session
app.get('/api/location/session/:sessionId', async (req, res) => {
  try {
    const sessionId = req.params.sessionId;
    const records = await convex.query('locationRecords:getLocationRecordsBySession', { sessionId });
    
    res.json({
      success: true,
      records: records,
      count: records.length
    });
    
  } catch (error) {
    console.error('Error fetching session records:', error);
    res.status(500).json({ 
      error: 'Failed to fetch session records',
      details: error.message 
    });
  }
});

// Get location records by time range
app.get('/api/location/range', async (req, res) => {
  try {
    const startTime = parseInt(req.query.startTime);
    const endTime = parseInt(req.query.endTime);
    const deviceId = req.query.deviceId;
    
    if (!startTime || !endTime) {
      return res.status(400).json({ error: 'startTime and endTime are required' });
    }
    
    const records = await convex.query('locationRecords:getLocationRecordsByTimeRange', {
      startTime,
      endTime,
      deviceId
    });
    
    res.json({
      success: true,
      records: records,
      count: records.length
    });
    
  } catch (error) {
    console.error('Error fetching records by time range:', error);
    res.status(500).json({ 
      error: 'Failed to fetch records by time range',
      details: error.message 
    });
  }
});

// Get device statistics
app.get('/api/stats/device/:deviceId', async (req, res) => {
  try {
    const deviceId = req.params.deviceId;
    const stats = await convex.query('locationRecords:getDeviceStats', { deviceId });
    
    res.json({
      success: true,
      stats: stats
    });
    
  } catch (error) {
    console.error('Error fetching device stats:', error);
    res.status(500).json({ 
      error: 'Failed to fetch device statistics',
      details: error.message 
    });
  }
});

// Delete old records
app.delete('/api/location/cleanup', async (req, res) => {
  try {
    const olderThanTimestamp = parseInt(req.body.olderThanTimestamp);
    const deviceId = req.body.deviceId;
    
    if (!olderThanTimestamp) {
      return res.status(400).json({ error: 'olderThanTimestamp is required' });
    }
    
    const result = await convex.mutation('locationRecords:deleteOldRecords', {
      olderThanTimestamp,
      deviceId
    });
    
    res.json({
      success: true,
      deletedCount: result.deletedCount,
      message: `Deleted ${result.deletedCount} old records`
    });
    
  } catch (error) {
    console.error('Error deleting old records:', error);
    res.status(500).json({ 
      error: 'Failed to delete old records',
      details: error.message 
    });
  }
});

// Start server
app.listen(port, '0.0.0.0', () => {
  console.log(`🚀 Location API Server running at http://localhost:${port}`);
  console.log(`📍 Available endpoints:`);
  console.log(`   POST /api/location - Insert location record`);
  console.log(`   GET  /api/location/recent - Get recent records`);
  console.log(`   GET  /api/location/device/:deviceId - Get records by device`);
  console.log(`   GET  /api/location/session/:sessionId - Get records by session`);
  console.log(`   GET  /api/location/range - Get records by time range`);
  console.log(`   GET  /api/stats/device/:deviceId - Get device statistics`);
  console.log(`   DELETE /api/location/cleanup - Delete old records`);
  console.log(`   GET  /health - Health check`);
  console.log(`💾 Connected to Convex database at: ${process.env.CONVEX_URL || 'http://localhost:3210'}`);
}); 