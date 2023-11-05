# secure-file-sharing
To set up your Java environment to use SSL, you need to import your SSL certificates into a Java keystore. Here's how you can do it:

1. Generate a keystore and self-signed certificate: If you don't have a certificate yet, you can generate a self-signed certificate using the keytool utility that comes with Java.
2. Here's an example command: keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048

3. 2. Set up the SSL context in the java application. //Already added 
