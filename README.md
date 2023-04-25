# ReadCsvApp


Java application to read CSV file and inserts it to a Database.

Config.properties is the configuration file to update CSV location to import and JDBC string with Username and Password


## Eclipse to compile JAR -Steps

In the Package Explorer view, right-click on the project and select Properties.
In the Properties dialog, select Java Build Path, then the Libraries tab.
Click Add External JARs to add any external libraries your project depends on.
Click OK to close the Properties dialog.
In the Package Explorer view, right-click on the project and select Export.
In the Export dialog, expand the Java node and select Runnable JAR file, then click Next.
In the Launch configuration dropdown, select the class that contains the main method you want to run.
Choose a destination for the JAR file and enter a name for it.
On the next screen, select the options you want for the JAR file, such as compression and manifest file settings.
Under Library handling, choose the option Extract required libraries into generated JAR.
Click Finish to generate the JAR file.
