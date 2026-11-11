import React, { useEffect, useState } from 'react';
const API = '/api/incidents';
export default function App() {
  const [incidents, setIncidents] = useState([]);
  const [form, setForm] = useState({title:'',description:'',serviceName:'',severity:'MEDIUM'});
  const [results, setResults] = useState({});
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(null);
  async function request(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) throw new Error('Request failed: ' + response.status);
    return response.json();
  }
  async function load() { try { setIncidents(await request(API)); } catch (e) { setError(e.message); } }
  useEffect(() => { load(); }, []);
  async function create(e) {
    e.preventDefault(); setError('');
    try {
      await request(API, {method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(form)});
      setForm({title:'',description:'',serviceName:'',severity:'MEDIUM'}); await load();
    } catch(e) { setError(e.message); }
  }
  async function investigate(id) {
    setBusy(id); setError('');
    try {
      const result = await request(API+'/'+id+'/investigate',{method:'POST'});
      setResults(previous=>({...previous,[id]:result})); await load();
    } catch(e) { setError(e.message); } finally { setBusy(null); }
  }
  async function updateStatus(id,status) {
    try {
      await request(API+'/'+id+'/status',{method:'PATCH',headers:{'Content-Type':'application/json'},body:JSON.stringify({status})}); await load();
    } catch(e) { setError(e.message); }
  }
  return <main><header><h1>Incident Intelligence</h1><p>Evidence-first, read-only incident investigation</p></header>
    <section><h2>Report an incident</h2><form onSubmit={create}>
      <input aria-label="Title" placeholder="Incident title" required value={form.title} onChange={e=>setForm({...form,title:e.target.value})}/>
      <input aria-label="Service" placeholder="Service name" required value={form.serviceName} onChange={e=>setForm({...form,serviceName:e.target.value})}/>
      <select aria-label="Severity" value={form.severity} onChange={e=>setForm({...form,severity:e.target.value})}>{['LOW','MEDIUM','HIGH','CRITICAL'].map(s=><option key={s}>{s}</option>)}</select>
      <textarea aria-label="Description" placeholder="Symptoms and recent changes" value={form.description} onChange={e=>setForm({...form,description:e.target.value})}/>
      <button type="submit">Create incident</button>
    </form></section>
    {error && <p role="alert">{error}</p>}
    <section><h2>Incidents</h2>{incidents.length===0 && <p>No incidents yet.</p>}
      {incidents.map(i=><article key={i.id}><div className="top"><h3>#{i.id} {i.title}</h3><span>{i.severity} · {i.status}</span></div><p>{i.serviceName} — {i.description}</p>
        <button disabled={busy===i.id} onClick={()=>investigate(i.id)}>{busy===i.id?'Investigating…':'Investigate'}</button>
        <select aria-label={'Status for '+i.id} value={i.status} onChange={e=>updateStatus(i.id,e.target.value)}>{['OPEN','INVESTIGATING','MITIGATED','RESOLVED'].map(s=><option key={s}>{s}</option>)}</select>
        {results[i.id] && <div className="report"><h4>Preliminary assessment</h4><p>{results[i.id].assessment}</p><h4>Recommended checks</h4><ul>{results[i.id].recommendations.map((a,n)=><li key={n}>{a}</li>)}</ul><h4>Runbook evidence</h4><ul>{results[i.id].runbooks.map((r,n)=><li key={n}><strong>{r.title}</strong>: {r.content}</li>)}</ul></div>}
      </article>)}</section></main>;
}