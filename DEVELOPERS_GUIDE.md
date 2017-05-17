# KaiZen OpenAPI Editor - Developer's Guide

# Building from Sources
Development of KaiZen Editor should be done with Eclipse. This project uses Maven/Tycho so you should have the Eclipse maven plugin `m2e` already installed.

## 1. Get sources
To start developing, clone the repository with the following command:

```
git clone git@github.com:RepreZen/KaiZen-OpenAPI-Editor.git
``` 

## 2. Import to Eclipse IDE
Open Eclipse and select `File > Import... > Maven > Existing Maven Project`. Then select the folder KaiZen-OpenAPI-Editor, 
and add the following projects into your current workspace:

<img width="663" alt="screen shot 2017-05-09 at 11 28 15 am" src="https://cloud.githubusercontent.com/assets/644582/25858815/b3248a80-34aa-11e7-826b-e6b5547a01ed.png">

## 3. Set the Target Platform

The target platform provides some third-party dependencies. Eclipse will show compilation errors if these dependencies are not resolved in the target platform. 

To activate the target platform, open `com.reprezen.swagedit.target/com.reprezen.swagedit.target.target` and click on "Set as Target Platform":

<img width="1042" alt="kaizen_targetplatform" src="https://cloud.githubusercontent.com/assets/644582/25860442/8cb79cf2-34af-11e7-931b-e87259e79fbe.png">

## 4. Launch the Solution

There are two ways to launch the KaiZen Editor solution:

### 4a. Launching from Sources

This is the fastest and easiest way to launch the solution from within the Eclipse IDE. It also provides debugging capabilities.

1. From Eclipse with the KaiZen-OpenAPI-Editor source project, open the **Launch configurations...** dialog, then right-click on the **Eclipse Application** located in the left side and choose **New**. 
2. Make sure that KaiZen-OpenAPI-Editor plugins are included to the configuration. You can do this by selecting **All workspace and enabled target plug-ins** in the **Plug-ins** tab.

### 4b. Launching with a Local Update Site

The previous option, launching from the source projects in Eclipse, may not faithfully reproduce the same runtime dependencies that end users will be working with. When launching in this manner, Eclipse often ignores `build.properties`, or includes additional resources that are not explicitly included in `build.properties`.  For example, icons can be displayed correctly when launched from Eclipse, but shown as red rectangles in the product. It happens if the containing folder was not added to the `build.properties`.

The other option is to launch the solution with a local update site, built from source code. This option allows us to test exactly what end users will get. Another advantage of using a locally built update site is that you can easily share it with other people.

From inside your KaiZen-OpenAPI-Editor folder, run the following command:

```
mvn clean verify
```

This command will build the project and generate an update site under the folder 
`KaiZen-OpenAPI-Editor/com.reprezen.swagedit.repository/target/repository`.

You can now install KaiZen OpenAPI Editor into your Eclipse by clicking on `Help > Install New Software... > Add...`
This will show a dialog box where you can select the location of the update site.
Click on `Local...` and select the folder `KaiZen-OpenAPI-Editor/com.reprezen.swagedit.repository/target/repository` and then click `Ok`.

You can now select and install KaiZen-OpenAPI-Editor from the Eclipse update manager.

# Style Guide
Please import [RepreZen Java Code Formatter](https://raw.githubusercontent.com/RepreZen/KaiZen-OpenAPI-Editor/master/etc/dev-env/ModSquad_formatter_profile.xml) to your Eclipse IDE.

# Architecture
![KaiZen OpenAPI Editor Architecture](https://cloud.githubusercontent.com/assets/644582/13757221/cf31b4e8-e9f9-11e5-8e6b-8aeb26fc3ac9.png)

# JSON Schema
Validation, code assist, outline, and quick outline are built based on JSON Schema which is analyzed by our model. You can find more details in [#198 KaiZen OpenAPI Editor Model](https://github.com/RepreZen/KaiZen-OpenAPI-Editor/issues/198).
![JSON Schema Model](http://i.imgur.com/h38zU2C.png)
### Validation
See `com.reprezen.swagedit.core.validation.Validator.isSchemaDefinition(AbstractNode)`
* Severity: warning vs error
* Message
* Line in the document

### Code assist: LabelProvider
See `com.reprezen.swagedit.assist.SwaggerProposalProvider.getProposals(TypeDefinition, AbstractNode, String)`
Given a documentOffset, we need:
* replacementString
* displayString
* description
* type (displayed after the “:”)

### Outline and QuickOutline: LabelProvider
See `com.reprezen.swagedit.editor.outline.OutlineStyledLabelProvider.getStyledString(AbstractNode)`
Labels are composed from the corresponding element in the JSON Schema, it’s based on the “title” property of the node, name of the containing property of the path to the property.
