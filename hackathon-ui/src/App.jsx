import { useEffect, useState } from 'react'
import './App.css'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

async function request(path, options = {}) {
  const response = await fetch(`${API_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })

  if (!response.ok) {
    const body = await response.json().catch(() => ({}))
    throw new Error(body.error || `Request failed with status ${response.status}`)
  }

  return response.status === 204 ? null : response.json()
}

function App() {
  const [agents, setAgents] = useState([])
  const [orders, setOrders] = useState([])
  const [suggestions, setSuggestions] = useState([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')
  const [actionId, setActionId] = useState(null)
  const [lastUpdated, setLastUpdated] = useState(null)

  async function loadData(showSpinner = false) {
    if (showSpinner) setRefreshing(true)
    try {
      const [agentData, orderData, suggestionData] = await Promise.all([
        request('/agents'),
        request('/orders'),
        request('/suggestions?status=PENDING'),
      ])
      setAgents(agentData)
      setOrders(orderData)
      setSuggestions(suggestionData)
      setError('')
      setLastUpdated(new Date())
    } catch (loadError) {
      setError(loadError.message)
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }

  useEffect(() => {
    const initialLoad = window.setTimeout(() => loadData(), 0)
    const interval = window.setInterval(() => loadData(), 5000)
    return () => {
      window.clearTimeout(initialLoad)
      window.clearInterval(interval)
    }
  }, [])

  async function updateSuggestion(id, status) {
    setActionId(id)
    try {
      await request(`/suggestions/${id}`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      await loadData()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      setActionId(null)
    }
  }

  async function updateAgentStatus(id, status) {
    setActionId(id)
    try {
      await request(`/agents/${id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ status }),
      })
      await loadData()
    } catch (actionError) {
      setError(actionError.message)
    } finally {
      setActionId(null)
    }
  }

  const orderById = new Map(orders.map((order) => [order.id, order]))
  const offlineCount = agents.filter((agent) => agent.status === 'OFFLINE').length
  const activeCount = agents.filter((agent) => agent.status !== 'OFFLINE').length

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <span className="brand-mark">ZR</span>
          <div>
            <p className="eyebrow">ZipRun / operations</p>
            <h1>Reassignment control room</h1>
          </div>
        </div>
        <div className="topbar-actions">
          <span className="live-status"><span className="pulse-dot" /> Live monitor</span>
          <button className="button button-quiet" type="button" onClick={() => loadData(true)} disabled={refreshing}>
            {refreshing ? 'Refreshing...' : 'Refresh now'}
          </button>
        </div>
      </header>

      {error && <div className="error-banner" role="alert">{error}</div>}

      <section className="summary-grid" aria-label="Operations summary">
        <div className="summary-card summary-card-primary">
          <span className="summary-label">Pending decisions</span>
          <strong>{suggestions.length}</strong>
          <span className="summary-note">Awaiting ops approval</span>
        </div>
        <div className="summary-card">
          <span className="summary-label">Agents online</span>
          <strong>{activeCount}</strong>
          <span className="summary-note">of {agents.length} rostered</span>
        </div>
        <div className="summary-card">
          <span className="summary-label">Offline events</span>
          <strong>{offlineCount}</strong>
          <span className="summary-note">Currently unresolved</span>
        </div>
        <div className="summary-card">
          <span className="summary-label">Orders tracked</span>
          <strong>{orders.length}</strong>
          <span className="summary-note">Across all statuses</span>
        </div>
        <div className="summary-card summary-card-time">
          <span className="summary-label">Last sync</span>
          <strong>{lastUpdated ? lastUpdated.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '--:--'}</strong>
          <span className="summary-note">Auto-refresh every 5 sec</span>
        </div>
      </section>

      <div className="workspace-grid">
        <section className="panel suggestions-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow accent-text">Decision queue</p>
              <h2>Reassignment suggestions</h2>
            </div>
            <span className="queue-count">{suggestions.length} pending</span>
          </div>

          {loading ? (
            <div className="empty-state"><span className="loader" />Loading queue</div>
          ) : suggestions.length === 0 ? (
            <div className="empty-state">
              <strong>Queue is clear</strong>
              <span>No pending decisions. {orders.length} orders remain tracked.</span>
            </div>
          ) : (
            <div className="suggestion-list">
              {suggestions.map((suggestion) => {
                const order = orderById.get(suggestion.orderId)
                const isBusy = actionId === suggestion.id
                return (
                  <article className="suggestion-card" key={suggestion.id}>
                    <div className="suggestion-main">
                      <div className="suggestion-title-row">
                        <span className="order-id">{suggestion.orderId}</span>
                        {suggestion.triggerReason === 'AGENT_OFFLINE' && (
                          <span className="event-badge">Re-plan / offline event</span>
                        )}
                      </div>
                      <h3>{order?.description || 'Order details loading'}</h3>
                      <p className="reasoning">“{suggestion.reasoning}”</p>
                    </div>
                    <div className="suggestion-meta">
                      <div>
                        <span className="meta-label">Recommended agent</span>
                        <strong>{suggestion.recommendedAgentId}</strong>
                      </div>
                      <div>
                        <span className="meta-label">Confidence</span>
                        <strong className="confidence">{Math.round(suggestion.confidence * 100)}%</strong>
                      </div>
                    </div>
                    <div className="suggestion-actions">
                      <button className="button button-approve" type="button" disabled={isBusy} onClick={() => updateSuggestion(suggestion.id, 'ACCEPTED')}>
                        {isBusy ? 'Working...' : 'Accept suggestion'}
                      </button>
                      <button className="button button-reject" type="button" disabled={isBusy} onClick={() => updateSuggestion(suggestion.id, 'REJECTED')}>
                        Reject
                      </button>
                    </div>
                  </article>
                )
              })}
            </div>
          )}
        </section>

        <aside className="panel roster-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow teal-text">Fleet status</p>
              <h2>Agent roster</h2>
            </div>
            <span className="roster-live">● synced</span>
          </div>
          <div className="roster-list">
            {agents.map((agent) => {
              const loadPct = agent.maxCapacity ? Math.min(100, Math.round((agent.activeOrderCount / agent.maxCapacity) * 100)) : null
              return (
                <div className="agent-row" key={agent.id}>
                  <div className="agent-row-top">
                    <div className="agent-identity">
                      <span className={`agent-avatar status-${agent.status.toLowerCase()}`}>{agent.name.charAt(0)}</span>
                      <div>
                        <strong>{agent.name}</strong>
                        <span>{agent.id} · {agent.activeOrderCount} active orders</span>
                      </div>
                    </div>
                    <select
                      aria-label={`Status for ${agent.name}`}
                      className={`status-select status-${agent.status.toLowerCase()}`}
                      value={agent.status}
                      disabled={actionId === agent.id}
                      onChange={(event) => updateAgentStatus(agent.id, event.target.value)}
                    >
                      <option value="AVAILABLE">Available</option>
                      <option value="BUSY">Busy</option>
                      <option value="OFFLINE">Offline</option>
                    </select>
                  </div>
                  <div className="agent-row-meta">
                    {agent.currentZone && <span className="zone-chip">{agent.currentZone}</span>}
                    {loadPct !== null && (
                      <div className="capacity-track" title={`${agent.activeOrderCount} of ${agent.maxCapacity} capacity`}>
                        <div className="capacity-fill" style={{ width: `${loadPct}%` }} />
                        <span className="capacity-label">{agent.activeOrderCount}/{agent.maxCapacity}</span>
                      </div>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
          <div className="roster-note">
            Set an assigned agent to <strong>Offline</strong> to trigger the asynchronous recovery loop.
          </div>
        </aside>
      </div>

      <section className="panel orders-panel">
        <div className="panel-heading">
          <div>
            <p className="eyebrow accent-text">Dispatch board</p>
            <h2>Order book</h2>
          </div>
          <span className="queue-count">{orders.length} tracked</span>
        </div>
        <div className="order-table">
          <div className="order-table-head">
            <span>Order</span>
            <span>Route</span>
            <span>Agent</span>
            <span>Status</span>
          </div>
          {orders.map((order) => (
            <div className="order-row" key={order.id}>
              <span className="order-id">{order.id}</span>
              <span className="order-route">
                {order.pickupZone && order.dropoffZone
                  ? <><span className="zone-chip">{order.pickupZone}</span> → <span className="zone-chip">{order.dropoffZone}</span></>
                  : order.description}
              </span>
              <span>{order.assignedAgentId}</span>
              <span className={`order-status status-${order.status.toLowerCase()}`}>{order.status}</span>
            </div>
          ))}
        </div>
      </section>
    </main>
  )
}

export default App
