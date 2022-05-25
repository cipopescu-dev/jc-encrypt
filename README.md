# jc-encrypt
## Requirements:
* JavaCard 3.1.0 tools and simulator
* `JC_HOME` variable set, on a folder containing both the tools and simulator (just extract jc tools over jc simulator)
* JDK 17 (note: target is `java 1.7` for the Applet, since language level 17 is not supported yet.)
* ClassPath set with `$JC_HOME/lib/*` 
* Custom `.vscode\settings.json` template:
  ```JSON
  {
    "java.project.referencedLibraries": [
        "c:\\Users\\cipdev\\jc310\\lib\\*" 
    ],
    "java.project.sourcePaths": [
        "applet/src",
        "host-app/src/"
    ]
  }
  ```

---
## Applet
* You should run every script from the `applet` directory
---
## Host App
* You should run every script from the `host-app` directory