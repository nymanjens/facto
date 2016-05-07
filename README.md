Family Accounting Tool
======================

## Deployment
```
# refresh application secret
./activator playUpdateSecret

# Build application
./activator dist

# Deploy files
cd /somewhere/you/want/the/files
unzip .../target/universal/facto-1.0-SNAPSHOT.zip
mv facto-1.0-SNAPSHOT/* .
rm -d facto-1.0-SNAPSHOT/

# Create database tables
bin/facto -DdropAndCreateNewDb
rm RUNNING_PID

# (Optional) Import Facto v1 backups
bin/facto -DloadFactoV1DataFromPath=path/to/backup.sql
rm RUNNING_PID

# Run application
bin/facto
```
