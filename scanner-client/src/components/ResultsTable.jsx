export default function ResultsTable({ results }) {
   if (!results || results.length === 0) return <p>No results found.</p>;

   const severityColor = (sev) => {
      switch(sev) {
         case 'HIGH': return '#dc3545';
         case 'MEDIUM': return '#ffc107';
         case 'LOW': return '#28a745';
         default: return '#6c757d';
      }
   };

   return (
       <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '10px' }}>
          <thead>
          <tr style={{ background: '#e9ecef' }}>
             <th style={thStyle}>Project</th>
             <th style={thStyle}>Issue Type</th>
             <th style={thStyle}>Severity</th>
             <th style={thStyle}>Details</th>
          </tr>
          </thead>
          <tbody>
          {results.map((r, i) => (
              <tr key={i} style={{ borderBottom: '1px solid #dee2e6' }}>
                 <td style={tdStyle}>{r.projectName}</td>
                 <td style={tdStyle}>{r.issueType}</td>
                 <td style={tdStyle}>
                            <span style={{
                               background: severityColor(r.severity),
                               color: r.severity === 'MEDIUM' ? '#000' : '#fff',
                               padding: '4px 12px',
                               borderRadius: '12px',
                               fontSize: '12px',
                               fontWeight: 'bold',
                               display: 'inline-block'
                            }}>
                                {r.severity}
                            </span>
                 </td>
                 <td style={tdStyle}>{r.details}</td>
              </tr>
          ))}
          </tbody>
       </table>
   );
}

const thStyle = { padding: '12px', textAlign: 'left', borderBottom: '2px solid #adb5bd' };
const tdStyle = { padding: '12px' };