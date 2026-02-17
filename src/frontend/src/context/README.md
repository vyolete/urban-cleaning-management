# Context Providers

React Context providers for global state management.

## Contexts to implement:

### AuthContext
Global authentication state:
```javascript
<AuthProvider>
  <App />
</AuthProvider>
```

Provides:
- `user` - Current user object
- `token` - JWT token
- `isAuthenticated` - Boolean
- `login(credentials)` - Login method
- `logout()` - Logout method
- `loading` - Loading state

### ThemeContext (Optional)
Theme/dark mode:
```javascript
<ThemeProvider>
  <App />
</ThemeProvider>
```

### NotificationContext (Optional)
Global notifications/toasts:
```javascript
const { showNotification } = useNotification();
showNotification('Success!', 'success');
```

## Usage Example

```javascript
// AuthContext.jsx
import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(localStorage.getItem('token'));
  
  // ... implementation
  
  return (
    <AuthContext.Provider value={{ user, token, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
```
