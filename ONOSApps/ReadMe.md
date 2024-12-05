
The testApp prooves that it is possible to create raw applications with the template generator 
from ONOS (onos-create-app )

The folders in the IntelliJ folder are different projects illustrating functionality of ONOS. 
They have been ported from course 34359 and adapted / modified to the version of ONOS installed
in the 5GR-project VM.
They have all been tested and are functional for the 5GR VM.
It is advisable to open them with IntelliJ IDE. It is possible to compile them and upload them to
 ONOS from the IDE's terminal as well, according to the instructions provided in the "Hints" file in the desktop of the VM.

They correspond to the examples in lectures 4, 5 and 6 of DTUs SDN course (34359): 
https://kurser.dtu.dk/course/34359

The scripts in the folder "scriptExamples" are related to some of those examples and generate 
Mininet topologies necessary for them.

The folder "ONOSCodesExamples" provides other examples of use of ONOS API, but be aware that 
they have not been ported to the new version of ONOS so, they will not compile properly.

An additional project has been added as a separated ZIP file, demonstrating the creation of virtual networks, via overlay tagging (VLAN) for a given topology. The script creating the topology in Mininet is included within the zipped file.

In case of issues, challenges or questions, please contact jose at joss@fotonik.dtu.dk

