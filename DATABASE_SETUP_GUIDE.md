# EGLA Location Tracker - Local Database Setup Guide

## 🎯 Overview

Your EGLA Location Tracker app has been successfully upgraded to use a **local Convex database** instead of CSV files. This provides:

- ✅ **Real-time data storage** on your local network
- ✅ **Better data structure** with indexing and queries
- ✅ **Multi-device support** - multiple phones can log to the same database
- ✅ **Automatic session tracking** and device identification
- ✅ **REST API** for advanced data analysis

---

## 🛠️ What Was Changed

### 1. **Database Server Setup** (`convex-db/` directory)
- **Convex Database**: Real-time database running locally
- **HTTP API Server**: REST API for Android app communication
- **Node.js Environment**: Sandboxed using NVS (Node Version Switcher)

### 2. **Android App Changes**
- **DatabaseLogger**: Replaces `CsvLogger` - sends data via HTTP API
- **Network Communication**: Uses Retrofit + OkHttp for reliable data transmission
- **Session Management**: Automatically tracks logging sessions and device IDs
- **Error Handling**: Graceful fallback when database is unavailable

---

## 🚀 Quick Start

### Step 1: Start the Database Server
```bash
cd convex-db
start-server.bat
```

This will start:
- **Convex Database** at `http://localhost:3210`
- **HTTP API Server** at `http://localhost:3000`

### Step 2: Configure Your Android Device

#### For Android Emulator:
- ✅ **No configuration needed** - uses `10.0.2.2:3000`

#### For Physical Android Device:
1. Find your computer's IP address:
   ```cmd
   ipconfig
   ```
2. Look for your WiFi adapter's IPv4 address (e.g., `192.168.1.100`)
3. Edit the `DatabaseLogger.kt` file:
   ```kotlin
   private val baseUrl = "http://192.168.1.YOUR_IP:3000"
   ```

### Step 3: Run Your Android App
- Start location tracking as usual
- The app will automatically connect to the database
- Look for **"✅ Database logging started"** in the logs

---

## 📊 API Endpoints

Your local database server provides these endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Check server status |
| `POST` | `/api/location` | Insert location record |
| `GET` | `/api/location/recent` | Get recent records |
| `GET` | `/api/location/device/:deviceId` | Get records by device |
| `GET` | `/api/location/session/:sessionId` | Get records by session |
| `GET` | `/api/stats/device/:deviceId` | Get device statistics |

### Example API Usage:

```bash
# Check server health
curl http://localhost:3000/health

# Get recent location records
curl http://localhost:3000/api/location/recent?limit=10

# Get statistics for a device
curl http://localhost:3000/api/stats/device/YOUR_DEVICE_ID
```

---

## 🔧 Database Schema

Your location data is stored with this structure:

```javascript
{
  timestamp: number,           // Unix timestamp
  latitude: number,            // Enhanced latitude
  longitude: number,           // Enhanced longitude
  originalLatitude: number,    // Original GPS latitude
  originalLongitude: number,   // Original GPS longitude
  accuracy: number,            // Enhanced accuracy (meters)
  originalAccuracy: number,    // Original GPS accuracy (meters)
  altitude: number,            // Altitude
  bearing: number,             // Direction bearing
  speed: number,               // Speed (m/s)
  direction: string,           // Human-readable direction
  isStationary: boolean,       // Whether device is stationary
  environment: string,         // Environment type (URBAN, SUBURBAN, etc.)
  operatingMode: string,       // EGLA operating mode
  accuracyImprovement: number, // Accuracy improvement percentage
  confidence: number,          // Confidence score
  processingTime: number,      // Processing time (ms)
  deviceId: string,            // Unique device identifier
  sessionId: string            // Logging session ID
}
```

---

## 🔍 Monitoring & Debugging

### Check Server Status
```bash
# Test the API server
curl http://localhost:3000/health

# Check if ports are in use
netstat -an | findstr :3000
netstat -an | findstr :3210
```

### Android App Logs
Look for these log messages in your app:
- ✅ `"Database logging started"`
- 📊 `"Device ID: xxxxx"`
- 📝 `"Session: session_xxxxx"`
- 💾 `"Location data sent to database"`
- ❌ `"Failed to send location data"` (check network connection)

### Common Issues & Solutions

#### ❌ "Failed to start database logging"
- **Solution**: Make sure the database server is running (`start-server.bat`)
- **Check**: Server logs for any errors

#### ❌ "Connection refused" errors
- **Solution**: Verify the IP address in `DatabaseLogger.kt` matches your computer's IP
- **Check**: Both devices are on the same WiFi network

#### ❌ "Failed to send location data"
- **Solution**: Check network connectivity between Android device and computer
- **Check**: Firewall settings allowing port 3000

---

## 📈 Advanced Usage

### Multiple Devices
- Each Android device gets a unique `deviceId`
- All devices can log to the same database simultaneously
- Use device-specific queries to separate data:
  ```bash
  curl http://localhost:3000/api/location/device/DEVICE_ID_1
  curl http://localhost:3000/api/location/device/DEVICE_ID_2
  ```

### Session Management
- Each time you start tracking, a new `sessionId` is created
- Sessions help group related location data
- Query by session:
  ```bash
  curl http://localhost:3000/api/location/session/SESSION_ID
  ```

### Data Export
- Query the API to export data in JSON format
- Create custom scripts to convert to CSV if needed
- Real-time analysis possible through API queries

---

## 🛡️ Security Notes

- **Local Network Only**: Database only accessible on your local network
- **No Authentication**: Designed for development/testing use
- **Firewall**: Windows may prompt to allow Node.js through firewall

---

## 🔄 Switching Back to CSV (If Needed)

If you need to switch back to CSV logging:

1. In `LocationTrackingViewModel.kt`, change:
   ```kotlin
   import com.example.eglatracker.utils.CsvLogger
   private val csvLogger = CsvLogger(application)
   ```

2. Update the logging calls to use `csvLogger` instead of `databaseLogger`

---

## 📞 Support

The database system is now fully integrated with your EGLA Location Tracker. 

**Key Benefits:**
- 🚀 **Faster Data Access**: Query specific time ranges, devices, or sessions
- 📊 **Better Analytics**: Built-in statistics and aggregation functions  
- 🔄 **Real-time Updates**: Data available immediately after logging
- 🌐 **Network Access**: Access your data from any device on the network

Your location tracking accuracy improvements from EGLA are now stored in a professional database system! 