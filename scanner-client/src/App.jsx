import {useState} from 'react';
import {
   lookupUser, lookupGroup,
   discoverUserProjects, discoverGroupProjects,
   scanProject
} from './services/api';
import ResultsTable from './components/ResultsTable';
import ExportButton from './components/ExportButton';

function App() {
   const [name, setName] = useState('');
   const [type, setType] = useState('user');
   const [token, setToken] = useState(''); // NEW: private token
   const [step, setStep] = useState(1);
   const [foundEntity, setFoundEntity] = useState(null);
   const [repos, setRepos] = useState([]);
   const [selectedRepo, setSelectedRepo] = useState(null);
   const [loading, setLoading] = useState(false);
   const [scanData, setScanData] = useState(null);
   const [error, setError] = useState('');

   // ===== STEP 1: LOOKUP =====
   const handleLookup = async (e) => {
      e.preventDefault();
      if (!name.trim()) return;
      setLoading(true);
      setError('');
      setFoundEntity(null);
      setRepos([]);
      setSelectedRepo(null);
      setScanData(null);
      setStep(1);

      try {
         if (type === 'user') {
            const res = await lookupUser(name, token);
            setFoundEntity({...res.data, type: 'user'});
         } else {
            const res = await lookupGroup(name, token);
            setFoundEntity({...res.data, type: 'group'});
         }
         setStep(2);
      } catch (err) {
         setError(`${type === 'user' ? 'User' : 'Group'} "${name}" not found. ${token ? '(Token provided)' : '(Public only — add token for private)'}`);
      } finally {
         setLoading(false);
      }
   };

   // ===== STEP 2: DISCOVER =====
   const handleDiscoverRepos = async () => {
      if (!foundEntity) return;
      setLoading(true);
      setError('');
      setRepos([]);
      setSelectedRepo(null);

      try {
         let res;
         if (foundEntity.type === 'user') {
            res = await discoverUserProjects(foundEntity.id, token);
         } else {
            res = await discoverGroupProjects(foundEntity.id, token);
         }
         setRepos(res.data);
         if (res.data.length === 0) {
            setError('No repositories found. If private repos exist, provide a token.');
         } else {
            setStep(3);
         }
      } catch (err) {
         setError('Failed to fetch repositories.');
      } finally {
         setLoading(false);
      }
   };

   // ===== STEP 3: SCAN =====
   const handleScan = async () => {
      if (!selectedRepo) return;
      setLoading(true);
      setError('');
      try {
         const res = await scanProject({
            gitlabUsername: name,
            projectId: selectedRepo.id,
            projectName: selectedRepo.name,
            projectUrl: selectedRepo.webUrl,
            token: token // passed in body as fallback
         }, token);
         setScanData(res.data);
         setStep(4);
      } catch (err) {
         setError('Scan failed: ' + (err.response?.data?.message || err.message));
      } finally {
         setLoading(false);
      }
   };

   return (
       <div style={{maxWidth: '900px', margin: '0 auto', padding: '40px 20px', fontFamily: 'sans-serif'}}>
          <h1>GitLab Repository Risk Scanner</h1>

          {/* ===== STEP 1 ===== */}
          <div style={{background: '#f8f9fa', padding: '20px', borderRadius: '8px', marginBottom: '20px'}}>
             <h3>Step 1: Find User or Group</h3>
             <form onSubmit={handleLookup}>
                <div style={{marginBottom: '10px'}}>
                   <label style={{marginRight: '15px'}}>
                      <input type="radio" value="user" checked={type === 'user'}
                             onChange={(e) => setType(e.target.value)}/> User
                   </label>
                   <label>
                      <input type="radio" value="group" checked={type === 'group'}
                             onChange={(e) => setType(e.target.value)}/> Group
                   </label>
                </div>
                <input
                    type="text"
                    placeholder={`Enter GitLab ${type} name`}
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    style={{padding: '10px', width: '250px', marginRight: '10px'}}
                />
                <input
                    type="password"
                    placeholder="Private Token (optional)"
                    value={token}
                    onChange={(e) => setToken(e.target.value)}
                    style={{padding: '10px', width: '200px', marginRight: '10px'}}
                    title="GitLab Personal Access Token for private repos"
                />
                <button type="submit" disabled={loading} style={{padding: '10px 20px'}}>
                   {loading ? 'Searching...' : 'Lookup'}
                </button>
             </form>
             <p style={{fontSize: '12px', color: '#6c757d', marginTop: '5px'}}>
                Leave token empty for public repos only. Enter a GitLab PAT to include private repos.
             </p>
          </div>

          {/* ===== STEP 2 ===== */}
          {step >= 2 && foundEntity && (
              <div style={{background: '#d4edda', padding: '20px', borderRadius: '8px', marginBottom: '20px'}}>
                 <h3>Step 2: {foundEntity.type === 'user' ? 'User' : 'Group'} Found</h3>
                 <p><strong>Name:</strong> {foundEntity.name}</p>
                 <p>
                    <strong>{foundEntity.type === 'user' ? 'Username' : 'Path'}:</strong> {foundEntity.type === 'user' ? foundEntity.username : foundEntity.path}
                 </p>
                 <button onClick={handleDiscoverRepos} disabled={loading} style={{padding: '10px 20px'}}>
                    {loading ? 'Fetching...' : 'Fetch Repositories'}
                 </button>
              </div>
          )}

          {/* ===== STEP 3 ===== */}
          {step >= 3 && repos.length > 0 && (
              <div style={{background: '#f8f9fa', padding: '20px', borderRadius: '8px', marginBottom: '20px'}}>
                 <h3>Step 3: Select Repository</h3>
                 <p>Found <strong>{repos.length}</strong> repositories</p>
                 <select
                     value={selectedRepo ? selectedRepo.id : ''}
                     onChange={(e) => {
                        const repo = repos.find(r => r.id === Number(e.target.value));
                        setSelectedRepo(repo || null);
                        setScanData(null);
                     }}
                     style={{padding: '10px', width: '400px', marginRight: '10px'}}
                 >
                    <option value="">-- Select a repository --</option>
                    {repos.map(repo => (
                        <option key={repo.id} value={repo.id}>
                           {repo.name}
                        </option>
                    ))}
                 </select>
                 <button onClick={handleScan} disabled={!selectedRepo || loading} style={{padding: '10px 20px'}}>
                    {loading ? 'Analyzing...' : 'Analyze Repository'}
                 </button>
              </div>
          )}

          {error && <p style={{
             color: '#dc3545',
             fontWeight: 'bold',
             padding: '10px',
             background: '#f8d7da',
             borderRadius: '4px'
          }}>{error}</p>}

          {/* ===== STEP 4 ===== */}
          {step >= 4 && scanData && (
              <div>
                 <h3>Scan Results for {scanData.results[0]?.projectName}</h3>
                 <ResultsTable results={scanData.results}/>
                 <ExportButton results={scanData.results} username={name}/>
              </div>
          )}
       </div>
   );
}

export default App;