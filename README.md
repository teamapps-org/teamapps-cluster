# TeamApps Cluster

## Key features
- Create or join a cluster with a single line of code.
- Encrypted by default either via shared secret or SSL certificate.
- Use **[TeamApps Message Protocol](https://github.com/teamapps-org/teamapps-message-protocol)** to define:
  - Message format
  - Services (RPC, pub/sub, etc.)
- Send messages that reference large files easily.
- Implement cluster services or consume services via generated code.
- Auto load-balance cluster services:
  - By measuring the load of each service (e.g. size of work queues).
  - By measuring the average processing speed.
  - Additionally by taking cpu usage of each service into account.
- Auto retry of service requests by network or node failure.
- Deliver error messages on bad input failures. 
