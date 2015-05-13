cd "/home/sketchlab/VirtualBox VMs/Shared/coursesketch/"
cd config
cd dev_info
cd github_auto

/bin/bash automerge.sh
cd ../../../
mvn clean install
cd config

/bin/bash copyjars.sh <<< "/home/sketchlab/VirtualBox VMs/Shared/" 
