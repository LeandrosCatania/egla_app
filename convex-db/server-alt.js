const express = require('express');
const { ConvexHttpClient } = require('convex/browser');
const cors = require('cors');

const app = express();
const port = 8080; // Alternative port that often bypasses router restrictions

// Initialize Convex client
const convex = new ConvexHttpClient(process.env.CONVEX_URL || 'http://localhost:3210');

// Middleware
app.use(cors());
app.use(express.json());

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy', 
    timestamp: new Date().toISOString(),
    port: port,
    message: 'EGLA API Server on alternative port 8080'
  });
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
      message: 'Location record saved successfully',
      server: 'Alternative port 8080'
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
      count: records.length,
      server: 'Alternative port 8080'
    });
    
  } catch (error) {
    console.error('Error fetching recent records:', error);
    res.status(500).json({ 
      error: 'Failed to fetch location records',
      details: error.message 
    });
  }
});

// All other endpoints from original server.js
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

// Start server
app.listen(port, '0.0.0.0', () => {
  console.log(`🚀 EGLA Location API Server (Alternative) running at http://localhost:${port}`);
  console.log(`🌐 Alternative port to bypass router restrictions`);
  console.log(`📍 Available endpoints:`);
  console.log(`   POST /api/location - Insert location record`);
  console.log(`   GET  /api/location/recent - Get recent records`);
  console.log(`   GET  /api/location/device/:deviceId - Get records by device`);
  console.log(`   GET  /api/location/session/:sessionId - Get records by session`);
  console.log(`   GET  /api/stats/device/:deviceId - Get device statistics`);
  console.log(`   GET  /health - Health check`);
  console.log(`💾 Connected to Convex database at: ${process.env.CONVEX_URL || 'http://localhost:3210'}`);
  console.log(`🔧 Use this server if port 3000 is blocked by router/firewall`);
}); 