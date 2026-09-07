import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

// Token is passed as a header for all requests
const getConfig = (token) => ({
   headers: token ? {'X-GitLab-Token': token} : {}
});

export const lookupUser = (username, token) =>
    axios.get(`${API_BASE}/lookup/user/${username}`, getConfig(token));

export const lookupGroup = (groupname, token) =>
    axios.get(`${API_BASE}/lookup/group/${groupname}`, getConfig(token));

export const discoverUserProjects = (userId, token) =>
    axios.get(`${API_BASE}/discover/user/${userId}/projects`, getConfig(token));

export const discoverGroupProjects = (groupId, token) =>
    axios.get(`${API_BASE}/discover/group/${groupId}/projects`, getConfig(token));

export const scanProject = (scanRequest, token) =>
    axios.post(`${API_BASE}/scan`, scanRequest, getConfig(token));

export const getLatestScan = (username) =>
    axios.get(`${API_BASE}/scan/${username}/latest`);

export const exportJson = (username) =>
    axios.get(`${API_BASE}/scan/${username}/export/json`);