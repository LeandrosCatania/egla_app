const express = require('express');
const { ConvexHttpClient } = require('convex/browser');
const cors = require('cors');

const app = express();
const port = 3001; // Different port to avoid conflicts

// Initialize Convex client
const convex = new ConvexHttpClient(process.env.CONVEX_URL || 'http://localhost:3210');

// Detailed logging middleware
app.use((req, res, next) => {
  const timestamp = new Date().toISOString();
  const clientIP = req.ip || req.connection.remoteAddress || req.socket.remoteAddress || 'unknown';
  const userAgent = req.get('User-Agent') || 'unknown';
  
  console.log(`[${timestamp}] 🔍 CONNECTION ATTEMPT:`);
  console.log(`  📍 Client IP: ${clientIP}`);
  console.log(`  🌐 Method: ${req.method}`);
  console.log(`  📝 URL: ${req.url}`);
  console.log(`  📱 User-Agent: ${userAgent}`);
  console.log(`  🔗 Headers:`, JSON.stringify(req.headers, null, 2));
  console.log('  ────────────────────────────────────────');
  
  next();
});

// Trust proxy for real IP detection
app.set('trust proxy', true);

// CORS with detailed logging
app.use(cors({
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: false
}));

app.use(express.json());

// Health check endpoint with detailed response
app.get('/health', (req, res) => {
  const response = {
    status: 'healthy',
    timestamp: new Date().toISOString(),
    server: 'Debug Server',
    port: port,
    clientIP: req.ip || req.connection.remoteAddress,
    headers: req.headers,
    message: '🔍 Debug server is working! Connection successful!'
  };
  
  console.log(`✅ HEALTH CHECK SUCCESS from ${req.ip || 'unknown'}`);
  res.json(response);
});

// Debug endpoint
app.get('/debug', (req, res) => {
  res.json({
    message: 'Debug endpoint working',
    timestamp: new Date().toISOString(),
    server: 'Debug Mode',
    connection: {
      clientIP: req.ip || req.connection.remoteAddress,
      userAgent: req.get('User-Agent'),
      protocol: req.protocol,
      host: req.get('host'),
      url: req.url
    }
  });
});

// Simple test endpoint
app.get('/test', (req, res) => {
  console.log(`🧪 TEST ENDPOINT HIT from ${req.ip || 'unknown'}`);
  res.send('TEST SUCCESSFUL - Android can reach the server!');
});

// Location endpoint (simplified for debugging)
app.post('/api/location', async (req, res) => {
  console.log(`📍 LOCATION DATA received from ${req.ip || 'unknown'}`);
  console.log(`📊 Data:`, JSON.stringify(req.body, null, 2));
  
  res.json({
    success: true,
    message: 'Location data received in debug mode',
    timestamp: new Date().toISOString(),
    clientIP: req.ip || req.connection.remoteAddress
  });
});

// Error handling
app.use((err, req, res, next) => {
  console.error(`❌ ERROR from ${req.ip || 'unknown'}:`, err);
  res.status(500).json({ error: 'Server error', details: err.message });
});

// Start server with detailed binding info
app.listen(port, '0.0.0.0', () => {
  console.log('🚀 ═══════════════════════════════════════');
  console.log('📡 EGLA DEBUG SERVER STARTED');
  console.log('🚀 ═══════════════════════════════════════');
  console.log(`🌐 Server running on port: ${port}`);
  console.log(`🔗 Local URL: http://localhost:${port}`);
  console.log(`📱 Network URLs:`);
  console.log(`   • http://192.168.1.14:${port}`);
  console.log(`   • http://172.29.16.1:${port}`);
  console.log(`   • http://10.0.2.2:${port} (Android Emulator)`);
  console.log('🔍 This server logs ALL connection attempts');
  console.log('📊 Test endpoints:');
  console.log(`   • GET  /health - Health check`);
  console.log(`   • GET  /test - Simple test`);
  console.log(`   • GET  /debug - Debug info`);
  console.log(`   • POST /api/location - Location data`);
  console.log('🚀 ═══════════════════════════════════════');
}); 