# Services

API service layer for communicating with the backend.

## Services to implement:

### api.js
Base Axios instance with:
- Base URL configuration
- Request interceptor (attach JWT token)
- Response interceptor (handle errors)

### authService.js
Authentication operations:
- `login(username, password)` - User login
- `register(userData)` - User registration
- `logout()` - Clear session
- `getCurrentUser()` - Get current user from token

### reportService.js
Report operations:
- `submitReport(reportData, photo)` - Submit new report
- `getMyReports()` - Get current user's reports
- `getAllReports()` - Get all reports (admin/operator)
- `getReportById(id)` - Get specific report

### taskService.js
Task operations:
- `getTasks(filters)` - Get tasks with filters
- `getTaskById(id)` - Get specific task
- `updateTaskState(id, newState)` - Update task state
- `getAuditHistory(id)` - Get task audit history

### configService.js
Configuration operations:
- `getCurrentConfig()` - Get current algorithm weights
- `updateWeights(weights)` - Update algorithm weights
- `getConfigHistory()` - Get configuration history

## Usage Example

```javascript
import { authService, reportService } from './services';

// Login
const response = await authService.login('username', 'password');

// Submit report
const report = await reportService.submitReport(data, photoFile);
```
