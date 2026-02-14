# Utility Functions

Helper functions and utilities used throughout the application.

## Utilities to implement:

### validators.js
Form validation functions:
```javascript
export const validateEmail = (email) => { ... };
export const validatePassword = (password) => { ... };
export const validateCoordinates = (lat, lon) => { ... };
export const validateRequired = (value) => { ... };
```

### formatters.js
Data formatting functions:
```javascript
export const formatDate = (date) => { ... };
export const formatDateTime = (datetime) => { ... };
export const formatPriority = (score) => { ... };
export const formatCategory = (category) => { ... };
```

### constants.js
Application constants:
```javascript
export const TASK_STATES = {
  PENDIENTE: 'PENDIENTE',
  ASIGNADO: 'ASIGNADO',
  EN_PROGRESO: 'EN_PROGRESO',
  RESUELTO: 'RESUELTO'
};

export const USER_ROLES = {
  CIUDADANO: 'ROLE_CIUDADANO',
  TECNICO: 'ROLE_TECNICO',
  ADMIN: 'ROLE_ADMIN'
};

export const CATEGORIES = [
  'RESIDUOS_PELIGROSOS',
  'VERTIDO_ILEGAL',
  'CONTENEDOR_DAÑADO',
  'ACUMULACION_BASURA',
  'GRAFITI',
  'MOBILIARIO_ROTO',
  'LIMPIEZA_GENERAL',
  'MANTENIMIENTO_JARDIN',
  'OTROS'
];
```

### mapHelpers.js
Map-related utilities:
```javascript
export const isWithinBounds = (lat, lon, bounds) => { ... };
export const calculateDistance = (point1, point2) => { ... };
export const getPriorityColor = (score) => { ... };
export const createMarkerIcon = (priority) => { ... };
```

### tokenHelpers.js
JWT token utilities:
```javascript
export const decodeToken = (token) => { ... };
export const isTokenExpired = (token) => { ... };
export const getUserFromToken = (token) => { ... };
```

## Usage Example

```javascript
import { validateEmail, formatDate, TASK_STATES } from '../utils';

const isValid = validateEmail('user@example.com');
const formatted = formatDate(new Date());
const state = TASK_STATES.PENDIENTE;
```
