import { useAuth } from 'react-oidc-context'
export function useToken() { const token=useAuth().user?.access_token; if (!token) throw new Error('Authenticated session is unavailable'); return token }
