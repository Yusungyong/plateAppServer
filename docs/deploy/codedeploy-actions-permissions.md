# CodeDeploy GitHub Actions Diagnostics Permissions

The deploy workflow can create and poll CodeDeploy deployments with the existing role. To print per-instance failure details in GitHub Actions, the role also needs read-only diagnostic permissions.

Role:

`arn:aws:iam::174977828951:role/GitHubActions-CodeDeploy`

Add these actions to the role policy:

```json
{
  "Effect": "Allow",
  "Action": [
    "codedeploy:ListDeploymentTargets",
    "codedeploy:BatchGetDeploymentTargets",
    "codedeploy:ListDeploymentInstances",
    "codedeploy:GetDeploymentInstance"
  ],
  "Resource": "*"
}
```

If your IAM policy must be tightly scoped, start with the deployment group ARN below and fall back to `"*"` only if AWS reports that the action does not support that resource scope.

`arn:aws:codedeploy:ap-northeast-2:174977828951:deploymentgroup:plate-app-server/plate-main-deploy-group`

These permissions do not fix a failed deployment by themselves. They allow the workflow to show which lifecycle hook failed and the CodeDeploy diagnostic log tail.
