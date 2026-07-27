import type { AuthProviderProps } from 'react-oidc-context'

export const oidcConfig: AuthProviderProps = {
  authority: 'http://localhost:8180/realms/monitoring',
  client_id: 'monitoring-app',
  redirect_uri: 'http://localhost:5173/',
  post_logout_redirect_uri: 'http://localhost:5173/',
  response_type: 'code',
  scope: 'openid profile email',
  automaticSilentRenew: true,
  onSigninCallback: () => window.history.replaceState({}, document.title, window.location.pathname)
}
