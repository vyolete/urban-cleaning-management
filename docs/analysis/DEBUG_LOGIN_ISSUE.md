# Debug Login Issue - Instructions

## Current Status

The login authentication flow has been updated with extensive debug logging to diagnose the "Acceso Denegado" issue.

## Steps to Debug

### 1. Clear Browser Cache and Storage

**IMPORTANT**: You must clear your browser cache and localStorage completely:

1. Open your browser (Chrome/Firefox/Safari)
2. Open Developer Tools (F12 or Cmd+Option+I on Mac)
3. Go to the **Application** tab (Chrome) or **Storage** tab (Firefox)
4. Under **Local Storage**, select `http://localhost:3000`
5. Click "Clear All" or delete all entries
6. Under **Session Storage**, do the same
7. Close Developer Tools
8. **Hard refresh** the page: Cmd+Shift+R (Mac) or Ctrl+Shift+R (Windows/Linux)

### 2. Open Browser Console

1. Open Developer Tools again (F12)
2. Go to the **Console** tab
3. Clear any existing logs

### 3. Test Login Flow

1. Navigate to `http://localhost:3000/login`
2. Enter credentials:
   - Username: `admin`
   - Password: `admin123`
3. Click "Iniciar Sesión"

### 4. Check Console Logs

You should see debug logs like:

```
Login response: {token: "...", role: "ROLE_ADMIN", username: "admin", ...}
Constructed user object: {username: "admin", role: "ROLE_ADMIN"}
User role: ROLE_ADMIN
Stored in localStorage - user: {"username":"admin","role":"ROLE_ADMIN"}
AuthContext - Setting user: {username: "admin", role: "ROLE_ADMIN"}
AuthContext - User role: ROLE_ADMIN
```

When redirected to `/dashboard`, you should see:

```
ProtectedRoute - User: {username: "admin", role: "ROLE_ADMIN"}
ProtectedRoute - User role: ROLE_ADMIN
ProtectedRoute - Required roles: ["ROLE_TECNICO", "ROLE_ADMIN"]
ProtectedRoute - Is authenticated: true
ProtectedRoute - Access granted
```

### 5. If You See "Access Denied"

Check the console for:

```
ProtectedRoute - Access denied: multiple roles check failed
ProtectedRoute - Has any role result: false
```

And look for the line showing your actual role:
```
Tu rol: [what role is shown here?]
```

### 6. Check localStorage Directly

In the Console tab, run:

```javascript
console.log('Token:', localStorage.getItem('token'));
console.log('User:', localStorage.getItem('user'));
const user = JSON.parse(localStorage.getItem('user'));
console.log('User role:', user.role);
console.log('Role type:', typeof user.role);
```

### 7. Check Network Tab

1. Go to the **Network** tab in Developer Tools
2. Filter by "XHR" or "Fetch"
3. Look for the `/api/auth/login` request
4. Click on it and check the **Response** tab
5. Verify the response contains: `{"token": "...", "role": "ROLE_ADMIN", "username": "admin", ...}`

## Expected Behavior

After login, you should:
1. See the dashboard page (not "Acceso Denegado")
2. See a list of tasks
3. Be able to interact with the operator dashboard

## Common Issues

### Issue 1: Old Code in Browser Cache
**Solution**: Hard refresh (Cmd+Shift+R) or clear cache completely

### Issue 2: localStorage Has Old Data
**Solution**: Clear localStorage as described in Step 1

### Issue 3: Token Expired
**Solution**: The token expires after 24 hours. Clear localStorage and login again.

### Issue 4: Wrong Role in Database
**Solution**: Check the database:
```bash
docker exec -it urbanclean-postgres psql -U urbanclean_user -d urbanclean -c "SELECT id, username, role FROM usuarios WHERE username='admin';"
```

Should show: `role | ROLE_ADMIN`

## Report Back

Please share:
1. All console logs from the login attempt
2. The localStorage contents (from Step 6)
3. The network response (from Step 7)
4. Screenshot of the error page if you still see "Acceso Denegado"

This will help identify exactly where the issue is occurring.
