# Usage Control System
This project implements the UCON framework.
It has been exported from https://gitlab.com/c3isp/others/new-ucs. Note that its artifacts cannot be reached anymore, after the Nexus repository manager (nexusc3isp.iit.cnr.it:8081) has been made accessible to CNR users only.
Therefore, from now on the artifacts will be deployed to the Package Registry of this gitLab instance (sssg-dev.iit.cnr.it), within the project new-ucs.

# deploy branch
The deploy branch includes enhanchements to the master branch, but should be revised.

# master branch
The master branch is the oldest working implementation of the UCON framework.
Besides all the underlying UCS core logic and components (PAP, PDP, SM, OM) implementations, which should probably be the same for all the use cases, it implements some PIPs (PIPReader, ...), a PEP (PEPRest),  and the UCS (UCSRest).
Nonetheless, interfaces for PIP, PEP, and UCS have been defined and can be used to create custom implementations, for your specific use case and project.

# integration branch
The integration branch has been created for integration with the [java implementation](https://bitbucket.org/marco-tiloca-sics/ace-java/src/master/) of the  [ACE framework](https://datatracker.ietf.org/doc/draft-ietf-ace-oauth-authz/).
