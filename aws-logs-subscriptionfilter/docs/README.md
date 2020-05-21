# AWS::Logs::SubscriptionFilter

Specifies a subscription filter and associates it with the specified log group.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "Type" : "AWS::Logs::SubscriptionFilter",
    "Properties" : {
        "<a href="#destinationarn" title="DestinationArn">DestinationArn</a>" : <i>String</i>,
        "<a href="#distribution" title="Distribution">Distribution</a>" : <i>String</i>,
        "<a href="#filtername" title="FilterName">FilterName</a>" : <i>String</i>,
        "<a href="#filterpattern" title="FilterPattern">FilterPattern</a>" : <i>String</i>,
        "<a href="#loggroupname" title="LogGroupName">LogGroupName</a>" : <i>String</i>,
        "<a href="#rolearn" title="RoleArn">RoleArn</a>" : <i>String</i>
    }
}
</pre>

### YAML

<pre>
Type: AWS::Logs::SubscriptionFilter
Properties:
    <a href="#destinationarn" title="DestinationArn">DestinationArn</a>: <i>String</i>
    <a href="#distribution" title="Distribution">Distribution</a>: <i>String</i>
    <a href="#filtername" title="FilterName">FilterName</a>: <i>String</i>
    <a href="#filterpattern" title="FilterPattern">FilterPattern</a>: <i>String</i>
    <a href="#loggroupname" title="LogGroupName">LogGroupName</a>: <i>String</i>
    <a href="#rolearn" title="RoleArn">RoleArn</a>: <i>String</i>
</pre>

## Properties

#### DestinationArn

The ARN of the destination to deliver matching log events to.

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Distribution

The method used to distribute log data to the destination.

_Required_: No

_Type_: String

_Pattern_: <code>^Random$|^ByLogStream$</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### FilterName

A name for the subscription filter.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>512</code>

_Pattern_: <code>[^:*]*</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### FilterPattern

A filter pattern for subscribing to a filtered stream of log events.

_Required_: Yes

_Type_: String

_Maximum_: <code>1024</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### LogGroupName

The name of the log group.

_Required_: Yes

_Type_: String

_Minimum_: <code>1</code>

_Maximum_: <code>512</code>

_Pattern_: <code>[\.\-_/#A-Za-z0-9]+</code>

_Update requires_: [Replacement](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-replacement)

#### RoleArn

The ARN of an IAM role that grants CloudWatch Logs permissions to deliver ingested log events to the destination stream.

_Required_: No

_Type_: String

_Minimum_: <code>1</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

