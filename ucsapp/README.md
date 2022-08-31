#  UCSApp

Android Application that implements the Usage Control System of the UCON model.

## Directories

 - assets/configs: contains json files for configuring UCSService components
 - assets/pip: contains pip files that can be used by PIPFileReader. NOTE: in order to being able to modify those files at runtime 
    application internal storage is used for reading PIP files and not assets folder. Use this folder only for copying files into /storage/emulated/0/Android/data/com.example.ucsintent/files
 - assets/xmls: contains policies and requests XACML files actually parsed by UCS component.