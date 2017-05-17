## Autoformat (Ctrl+Shift+F) removes YAML comments 
This is a [known issue](https://github.com/oyse/yedit/issues/15) of YEdit, the YAML Editor on which KaiZen OpenAPI Editor is built. There is no fix for this yet, so we recommend that you disable the key binding for YEdit in Eclipse Preferences:
<img width="954" alt="yedit_disableformat" src="https://cloud.githubusercontent.com/assets/644582/13615520/b0411e3a-e543-11e5-93d7-dd4917be20da.png">

1. Open the "General > Keys" page.
2. Type "format" in the search box.
3. Select the command from the YEdit category.
4. Click the "Unbind Command" button.
5. Confirm by selecting "OK".

## Handling special (non-UTF) characters
JSON does not support non UTF encoding, so for now we recommend avoid using special characters, e.g. accented letters, in your Swagger specification. See [issue 158](https://github.com/RepreZen/SwagEdit/issues/158) for details
