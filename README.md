<img src="https://cdn2.hubspot.net/hubfs/597611/Assets_Swagger/KZOE.png" alt="KaiZen OpenAPI Editor Logo" height="50%" width="50%"/>

# KaiZen OpenAPI Editor for Eclipse

KaiZen OpenAPI Editor is an Eclipse editor for the [Swagger](http://swagger.io) API Description Language, now known as the [OpenAPI Specification](http://openapis.org). The editor supports Swagger-OpenAPI version 2.0, with OpenAPI 3.0 support coming soon.

KaiZen Editor, formerly known as SwagEdit, was developed for [RepreZen API Studio](http://reprezen.com/swagger-tools), a comprehensive solution for API modeling, documentation, visualization, testing and code generation, built on Eclipse.

We welcome your suggestions and contributions!

## Feature Highlights
<img src="/etc/img/ContentAssistQuickOutline.png" alt="Drawing" width="400" />

### Validation
<img src="http://i.imgur.com/GrFw9EM.png" alt="Validation_screenshot" width="400">

### Code Assist
Code templates:  
<img src="http://i.imgur.com/ZtHJX6A.gif" alt="Code_template" width="400">

Keywords and values:  
<img src="http://i.imgur.com/3uZ5bQa.gif" alt="CodeAssist_keys_and_values" width="400">

### Code Assist for References
Code assist for references has several scopes which can be viewed in sequence by pressing `Ctrl`+`Space` repeatedly:

* The first scope shows only elements from the current document.
* The second expands it to elements from the containing project.
* The third shows elements from the entire workspace.

<img src="http://i.imgur.com/P0IWIEt.gif" alt="CodeAssist_for_references" width="400">

Pressing the hotkey a fourth time starts the cycle over again, with document scope.

### Navigation to a Reference
You can navigate to a reference using `Ctrl`+`Click`:  
<img src="http://i.imgur.com/7WpuV3K.gif" alt="Navigation_to_references" width="400">

### Quick Outline
Quick Outline can be invoked with `Ctrl`+`o`. Similar to code assist for references, it has three scopes: model, project, and workspace. It also allows filtering:    
<img src="http://i.imgur.com/jvcoooa.gif" alt="Navigation_to_references" width="400">

### Outline
Outline View shows the contents of the active OpenAPI spec:  
<img src="http://i.imgur.com/iv49CLn.png" alt="Navigation_to_references" width="400">

## Installing KaiZen OpenAPI Editor
KaiZen OpenAPI Editor requires Eclipse Mars.2 or higher.

### Installing from Eclipse Marketplace
The [Eclipse Marketplace solution](https://marketplace.eclipse.org/content/kaizen-openapi-editor) is the easiest way to install KaiZen Editor into an Eclipse IDE. You can drag-and-drop the Install button from the browser into your Eclipse IDE, or use the built-in Eclipse Marketplace Client.

### Installing from the Update Site 
You can install KaiZen OpenAPI Editor into your Eclipse IDE by clicking on `Help > Install New Software... > Add...`
This will show a dialog box where you can select the location of the update site.
Use the update site from (http://products.reprezen.com/swagedit/latest/) as the URL.

### Installing with RepreZen API Studio
KaiZen Editor is fully functional inside RepreZen API Studio, which adds live documentation and diagram views, sandbox testing with the built-in mock service and Swagger-UI, powerful code generation, and other features. See the RepreZen API Studio [video and feature tour](http://www.reprezen.com/swagger-tools) to learn more and download a free trial.

## Troubleshooting
See the [Troubleshooting Guide](https://github.com/RepreZen/SwagEdit/blob/master/TROUBLESHOOTING.md) for solutions to common problems.

## Contributing to KaiZen OpenAPI Editor
We welcome contributions - documentation, bug reports or bug fixes.
If you are interested in contributing to KaiZen Editor, please see the [Developer's Guide](https://github.com/RepreZen/SwagEdit/blob/master/DEVELOPERS_GUIDE.md). 

We also created a list of [good first bugs](https://github.com/RepreZen/SwagEdit/labels/Good%20First%20Bug)
that are relatively easy to fix.

## License
KaiZen OpenAPI Editor is provided under the [Eclipse Public License v1.0](https://www.eclipse.org/legal/epl-v10.html)

## Video: KaiZen Editor in RepreZen API Studio

[![Editing Swagger-OpenAPI in RepreZen API Studio](http://img.youtube.com/vi/KX_tHp_KQkE/0.jpg)](https://www.youtube.com/watch?v=KX_tHp_KQkE)

_**Note:** KaiZen Editor includes code assist, real-time validation, syntax highlighting, and outline view.<br/>
[Eclipse Color Theme](https://marketplace.eclipse.org/content/eclipse-color-theme) and [EditBox](http://marketplace.eclipse.org/content/nodeclipse-editbox-background-colors-themes-highlight-code-blocks-c-java-javascript-python) are available as separate plugins.<br/>
[RepreZen API Studio](http://reprezen.com/swagger-tools) includes the mock service, live Swagger-UI & other features that are not part of KaiZen Editor._

