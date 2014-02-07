name: 'svy-textfield',
displayName: 'Text field',
definition: 'servoydefault/textfield/textfield.js',
model:
{
        placeholderText : 'tagstring', 
        enabled : 'boolean', 
        visible : 'boolean', 
        styleClass : 'string', 
        text : 'tagstring', 
        margin : 'dimension', 
        printable : 'boolean', 
        valuelistID : { type: 'valuelist', for: 'dataProviderID'}, 
        verticalAlignment : {type:'number', values:[{DEFAULT:-1}, {TOP:1}, {CENTER:2} ,{BOTTOM:3}]}, 
        horizontalAlignment : {type:'number', values:[{DEFAULT:-1}, {LEFT:0}, {CENTER:2},{RIGHT:4}]}, 
        transparent : 'boolean', 
        tabSeq : 'tabseq', 
        selectOnEnter : 'boolean', 
        scrollbars : 'number', 
        location : 'point', 
        size : 'dimension', 
        useRTF : 'boolean', 
        format : {for:'dataProviderID' , type:'format'}, 
        fontType : 'font', 
        editable : 'boolean', 
        borderType : 'border', 
        dataProviderID : { 'type':'dataprovider', 'ondatachange': { 'onchange':'onDataChangeMethodID', 'callback':'onDataChangeCallback'}}, 
        toolTipText : 'tagstring', 
        foreground : 'color', 
        background : 'color' 
},
handlers:
{
        onRenderMethodID : 'function', 
        onRightClickMethodID : 'function', 
        onDataChangeMethodID : 'function', 
        onFocusLostMethodID : 'function', 
        onFocusGainedMethodID : 'function', 
        onActionMethodID : 'function' 
},
api:
{
        getAbsoluteFormLocationY:{
            returns: 'number',
            parameters:[]
        }, 
        getClientProperty:{
            returns: 'object',
            parameters:[ {'key':'object','optional':'false'}]
        }, 
        getDataProviderID:{
            returns: 'string',
            parameters:[]
        }, 
        getDesignTimeProperty:{
            returns: 'object',
            parameters:[ {'unnamed_0':'string','optional':'false'}]
        }, 
        getElementType:{
            returns: 'string',
            parameters:[]
        }, 
        getHeight:{
            returns: 'number',
            parameters:[]
        }, 
        getLabelForElementNames:{
            returns: 'string []',
            parameters:[]
        }, 
        getLocationX:{
            returns: 'number',
            parameters:[]
        }, 
        getLocationY:{
            returns: 'number',
            parameters:[]
        }, 
        getName:{
            returns: 'string',
            parameters:[]
        }, 
        getSelectedText:{
            returns: 'string',
            parameters:[]
        }, 
        getValueListName:{
            returns: 'string',
            parameters:[]
        }, 
        getWidth:{
            returns: 'number',
            parameters:[]
        }, 
        putClientProperty:{
            returns: 'void',
            parameters:[ {'key':'object','optional':'false'}, {'value':'object','optional':'false'}]
        }, 
        replaceSelectedText:{
            returns: 'void',
            parameters:[ {'s':'string','optional':'false'}]
        }, 
        requestFocus:{
            returns: 'void',
            parameters:[ {'mustExecuteOnFocusGainedMethod':'boolean','optional':'true'}]
        }, 
        selectAll:{
            returns: 'void',
            parameters:[]
        }, 
        setLocation:{
            returns: 'void',
            parameters:[ {'x':'number','optional':'false'}, {'y':'number','optional':'false'}]
        }, 
        setSize:{
            returns: 'void',
            parameters:[ {'width':'number','optional':'false'}, {'height':'number','optional':'false'}]
        }, 
        setValueListItems:{
            returns: 'void',
            parameters:[ {'value':'object','optional':'false'}]
        } 
}