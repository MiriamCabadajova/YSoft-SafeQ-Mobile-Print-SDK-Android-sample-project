# YSoft SafeQ Mobile Print SDK Android
## Sample mobile application
This is a sample mobile application for the Android platform depicting the usage of the mobile SDK developed for the YSoft Corporation a.s., for accessing the functionality of SafeQ product.

The main functionality included in the SDK is the login and upload to the YSoft SafeQ using two main channels - End User Interface and the Mobile Integration Gateway.

The mobile SDK is supplied in the *sdk* folder. It includes *Login.kt* and *Upload.kt* classes comprising the core functionality of SDK and *Printjob.kt* class representing the print jobs to be uploaded to the YSoft SafeQ. The *sdk* folder also contains *CustomTrust.java* class for resolving YSoft-signed certificate issues and also *IppEmptyRequest.kt* and *IppRequest.kt* classes implemented by Petr Bartoň as a part of his Master's thesis, for building MIG rewuests. Their integration into the code is shown in the *LoginActivity.kt* and *UploadActivity.kt* classes. The application contains only basic UI elements as they will be chosen and modified by software engineers developing the mobile application with their custom assets. 

You can launch the application by opening the topmost folder through the Android Studio IDE. 

For more detailed instructions on how to integrate the mobile SDK into your iOS application and utilize its functionality, see the bachelor's thesis... 
