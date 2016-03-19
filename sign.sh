rm -f signed.apk
java -jar mstar_system_sign_tool/signapk.jar mstar_system_sign_tool/platform.x509.pem mstar_system_sign_tool/platform.pk8  app/build/outputs/apk/app-debug.apk signed.apk
