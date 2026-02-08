# Pages

Top-level page components that compose the application views.

## Pages to implement:

### Public Pages
- **LoginPage** - User login
- **RegisterPage** - User registration (optional)
- **HomePage** - Landing page

### Citizen Pages
- **CitizenReportPage** - Submit new report
- **MyReportsPage** - View user's reports

### Operator Pages
- **OperatorDashboard** - Main operator interface
- **TaskDetailPage** - Detailed task view

### Admin Pages
- **AdminDashboard** - Admin control panel
- **ConfigPage** - Algorithm configuration
- **UserManagementPage** - User administration

## Routing Structure

```
/                       - Home/Landing
/login                  - Login
/citizen/report         - Submit report (protected)
/citizen/my-reports     - My reports (protected)
/operator/dashboard     - Operator dashboard (protected, TECNICO)
/operator/tasks/:id     - Task detail (protected, TECNICO)
/admin/dashboard        - Admin dashboard (protected, ADMIN)
/admin/config           - Configuration (protected, ADMIN)
```
