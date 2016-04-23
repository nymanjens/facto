Family Accounting Tool
======================

## Deployment
```
# Build application
activator stage

# Create database tables
target/universal/stage/bin/facto -DdropAndCreateNewDb
rm target/universal/stage/RUNNING_PID

# (Optional) Import Facto v1 backups
target/universal/stage/bin/facto -DloadFactoV1DataFromPath=path/to/backup.sql
rm target/universal/stage/RUNNING_PID

# Run application
target/universal/stage/bin/facto
```
