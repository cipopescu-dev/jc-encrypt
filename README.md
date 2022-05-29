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
# Applet
It sets it's password and AES key during the creation process, using the `CREATE` apdu. Checkout [the APDU examples](AesApplet/apdu_scripts/examples.md)

There are 4 main instructions:
1. Initialization  of the cipher (encrypt / decrypt)
2. Cipher `.update(..)` callable multiple times for a maximum block size of 128Bytes. 
3. Cipher `.doFinal(..)` usually called only one time. Will pad with zeros!
4. Validation of password 

> Padding notice: Since Oracle card Simulator does not support any kind of padding for `AES-CBC`, the applet will append `0x00`s to the last set of bytes until it reach the 16-byte block size required for an `AES128` key.  We can easily store the initial size of the message at the start (or end) of the encrypted message.
# Host App
Using `org.apache.commons.cli` I managed to implement a nice interface for the HostApp:
```
usage: java com.cipdev.jcapp.App -i inputFile
 -d,--decrypt        Decrypt the give input file. If not given, will
                     default to encryption mode
 -h,--host <arg>     Host of the device running the JCApplet. Defaults to
                     "localhost"
 -i,--input <arg>    Input file path. Will be send to the device running
                     the JCApplet
 -o,--output <arg>   Output file path. Will store the result of the device
                     running the JCApplet process. Defaults to input
                     filename + '.out'
 -p,--port <arg>     Port of the device running the JCApplet. Defaults to
                     "9025"
```
Long story short:
1. It takes it's `args` from the user
2. Tries to initiate a connection to a device running the JCApplet
3. Will ask for a password
4. Will proceed to encrypt or decrypt.
> At any given point, if one instruction fail because of a known isse (ex: pad password) the process will exit with `code 1` and show a message on screen.
# Testing
1. Open `AesApplet` folder in eclipse ('cause of JCard support) and build the project 
2. Open `HostApp` folder in any Java IDE and build the project
3. Start the applet
4. Copy-paste the creation scripts. Make sure you send the `create` apdu with a password and an AES-key
5. Start the `HostApp`'s main.
> If you want to skip step 1 2 3 and 4 you can start the simulator with a given memory file via CLI using the following:
```
cref_tdual -i AesApplet/simulator_roms/AesApplet.eeprom
```