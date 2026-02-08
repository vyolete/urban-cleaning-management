# Urban Cleaning Management - Frontend

React frontend application for the Urban Cleaning Management System.

## Prerequisites

- Node.js 18+
- npm or yarn

## Running Locally

1. **Install dependencies**
   ```bash
   npm install
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env with your configuration
   ```

3. **Run development server**
   ```bash
   npm run dev
   ```

The application will be available at `http://localhost:3000`

## Building for Production

```bash
npm run build
```

The build artifacts will be in the `dist/` directory.

## Project Structure

```
src/
├── components/          # Reusable components
│   ├── common/         # Generic components
│   ├── citizen/        # Citizen-specific components
│   ├── operator/       # Operator-specific components
│   └── admin/          # Admin-specific components
├── pages/              # Page components
├── services/           # API service layer
├── hooks/              # Custom React hooks
├── context/            # React Context providers
└── utils/              # Utility functions
```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint
