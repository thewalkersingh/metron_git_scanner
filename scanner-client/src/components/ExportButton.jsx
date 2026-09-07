export default function ExportButton({ results, username }) {
   const handleExport = () => {
      const dataStr = JSON.stringify(results, null, 2);
      const blob = new Blob([dataStr], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `scan-${username}-${new Date().toISOString().split('T')[0]}.json`;
      a.click();
      URL.revokeObjectURL(url);
   };

   return (
       <button onClick={handleExport} style={{ marginTop: '10px', padding: '8px 16px' }}>
          Export JSON
       </button>
   );
}