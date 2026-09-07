import {useState} from 'react';
import {scanUser} from '../services/api';

export default function ScanForm({onScanComplete}) {
   const [username, setUsername] = useState('');
   const [loading, setLoading] = useState(false);

   const handleSubmit = async (e) => {
      e.preventDefault();
      if (!username.trim()) return;
      setLoading(true);
      try {
         const res = await scanUser(username);
         onScanComplete(res.data);
      } catch (err) {
         alert('Scan failed: ' + err.message);
      } finally {
         setLoading(false);
      }
   };

   return (
       <form onSubmit={handleSubmit} style={{marginBottom: '20px'}}>
          <input
              type="text"
              placeholder="Enter GitLab username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              style={{padding: '10px', width: '300px', marginRight: '10px'}}
          />
          <button type="submit" disabled={loading} style={{padding: '10px 20px'}}>
             {loading ? 'Scanning...' : 'Scan'}
          </button>
       </form>
   );
}