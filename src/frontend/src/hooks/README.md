# Custom Hooks

Reusable React hooks for common functionality.

## Hooks to implement:

### useAuth
Authentication state and methods:
```javascript
const { user, isAuthenticated, login, logout, loading } = useAuth();
```

### useGeolocation
Browser geolocation API:
```javascript
const { location, error, loading, getCurrentLocation } = useGeolocation();
// location: { latitude, longitude }
```

### useTasks
Task management:
```javascript
const { tasks, loading, error, fetchTasks, updateTask } = useTasks(filters);
```

### useForm
Form state management:
```javascript
const { values, errors, handleChange, handleSubmit, reset } = useForm(
  initialValues,
  validationSchema,
  onSubmit
);
```

### useDebounce
Debounce values:
```javascript
const debouncedValue = useDebounce(value, delay);
```

### useLocalStorage
Persist state in localStorage:
```javascript
const [value, setValue] = useLocalStorage('key', defaultValue);
```

## Usage Example

```javascript
import { useAuth, useGeolocation } from '../hooks';

function MyComponent() {
  const { user, isAuthenticated } = useAuth();
  const { location, loading } = useGeolocation();
  
  if (!isAuthenticated) return <Redirect to="/login" />;
  if (loading) return <Spinner />;
  
  return <div>User: {user.username}, Location: {location.latitude}</div>;
}
```
