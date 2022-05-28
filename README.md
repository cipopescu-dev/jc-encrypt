# jc-encrypt
## Requirements:
* JavaCard 3.1.0 tools and simulator
* `JC_HOME` variable set, on a folder containing both the tools and simulator (just extract jc tools over jc simulator)
* JDK 17 (note: target is `java 1.7` for the Applet, since language level 17 is not supported yet.)
* ClassPath set with `$JC_HOME/lib/*` 
* Custom `.vscode\settings.json` template to ignore classpath errors in vscode
  ```JSON
  {
    "java.project.referencedLibraries": [
        "c:\\Users\\cipdev\\jc310\\lib\\*" 
    ],
    "java.project.sourcePaths": [
        "AesApplet/src"
    ]
  }
  ```

---
## Applet
It sets it's password and AES key during the creation process, using the `CREATE` apdu. Checkout [the APDU examples](AesApplet/apdu_scripts/examples.md)

There are 4 main instructions:
1. Initialization  of the cipher (encrypt / decrypt)
2. Cipher `.update(..)` callable multiple times for a maximum block size of 128Bytes. 
3. Cipher `.doFinal(..)` used **only** for the last 16B (or less) of the file
4. Validation of password 

> Padding notice: Since Oracle card Simulator does not support any kind of padding for `AES-CBC`, the applet will append `0x00`s to the last set of bytes until it reach the 16-byte block size required for an `AES128` key.  We can easily store the initial size of the message at the start (or end) of the encrypted message.
---
## Host App
