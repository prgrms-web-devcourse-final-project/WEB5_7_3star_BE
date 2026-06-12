from diagrams import Cluster, Diagram, Edge, Node
from diagrams.aws.compute import EC2
from diagrams.aws.devtools import Codedeploy
from diagrams.aws.network import ALB
from diagrams.aws.storage import S3
from diagrams.onprem.ci import GithubActions
from diagrams.onprem.client import Users
from diagrams.onprem.database import PostgreSQL
from diagrams.onprem.inmemory import Redis
from diagrams.onprem.monitoring import Grafana, Prometheus
from diagrams.onprem.vcs import Github
from diagrams.programming.framework import Spring

graph_attr = {
    "fontsize": "24",
    "pad": "1.0",
    "splines": "line",
    "nodesep": "1.5",
    "ranksep": "1.3",
    "margin": "0.5",
    "dpi": "170",
}

node_attr = {
    "fontsize": "19",
    "margin": "0.20",
}

edge_attr = {
    "fontsize": "20",
}

cluster_attr = {
    "margin": "20",
    "fontsize": "22",
}

instance_cluster_attr = {
    "margin": "8",
    "fontsize": "22",
}

asg_cluster_attr = {
    "margin": "15",
    "fontsize": "24",
}

with Diagram(
    "TrainUs Architecture",
    show=False,
    filename="trainus-architecture",
    outformat="png",
    direction="LR",
    graph_attr=graph_attr,
    node_attr=node_attr,
    edge_attr=edge_attr,
):
    with Cluster("CI/CD", graph_attr=cluster_attr):
        github = Github("GitHub")
        actions = GithubActions("Actions")
        artifact = S3("S3\nartifact/env")
        codedeploy = Codedeploy("CodeDeploy")
        github >> actions >> artifact >> codedeploy

    client = Users("Client\nJMeter")
    alb = ALB("ALB")

    with Cluster("API Auto Scaling Group", graph_attr=asg_cluster_attr):
        with Cluster("Instance 1", graph_attr=instance_cluster_attr):
            api_ec2_1 = EC2("EC2")
            api_app_1 = Spring("API\nSpring Boot")
            api_ec2_1 - api_app_1

        with Cluster("Instance n", graph_attr=instance_cluster_attr):
            api_ec2_n = Node(
                "EC2",
                shape="box",
                style="rounded,dashed",
                width="1.42",
                height="0.28",
                fixedsize="true",
                fontsize="19",
            )
            api_app_n = Node(
                "Spring Boot",
                shape="box",
                style="rounded,dashed",
                width="1.42",
                height="0.28",
                fixedsize="true",
                fontsize="19",
            )
            api_ec2_n - api_app_n

    with Cluster("TrainUs-Redis-Server", graph_attr=cluster_attr):
        redis_core = Redis("redis-core\nstock/waiting")
        redis_mq = Redis("redis-mq\nstream/status")

    with Cluster("TrainUs-Data-Server", graph_attr=cluster_attr):
        postgres = PostgreSQL("PostgreSQL\n(PostGIS)")

    with Cluster("Consumer Auto Scaling Group", graph_attr=asg_cluster_attr):
        with Cluster("Instance 1", graph_attr=instance_cluster_attr):
            consumer_ec2_1 = EC2("EC2")
            consumer_app_1 = Spring("Consumer\nSpring Boot")
            consumer_ec2_1 - consumer_app_1

        with Cluster("Instance n", graph_attr=instance_cluster_attr):
            consumer_ec2_n = Node(
                "EC2",
                shape="box",
                style="rounded,dashed",
                width="1.42",
                height="0.28",
                fixedsize="true",
                fontsize="19",
            )
            consumer_app_n = Node(
                "Spring Boot",
                shape="box",
                style="rounded,dashed",
                width="1.42",
                height="0.28",
                fixedsize="true",
                fontsize="19",
            )
            consumer_ec2_n - consumer_app_n

    with Cluster("Monitoring", graph_attr=cluster_attr):
        prometheus = Prometheus("Prometheus")
        grafana = Grafana("Grafana")
        prometheus << grafana

    client >> Edge(minlen="2") >> alb
    alb >> Edge(minlen="2") >> api_ec2_1
    alb >> Edge(minlen="2") >> api_ec2_n

    api_app_1 >> Edge(label="apply request", fontsize="20") >> redis_core
    api_app_1 >> Edge(label="poll status", fontsize="20") >> redis_mq
    api_app_1 >> Edge(label="read/write", color="darkgreen", fontsize="20") >> postgres

    redis_core >> Edge(label="admit", fontsize="20") >> consumer_app_1
    consumer_app_1 >> Edge(label="XREAD / ACK", fontsize="20") >> redis_mq
    consumer_app_1 >> Edge(label="batch insert", fontsize="20") >> postgres

    prometheus << Edge(label="metrics", style="dashed", fontsize="20") << [api_app_1, consumer_app_1]

    codedeploy >> Edge(label="deploy api", color="blue", style="dashed", constraint="false", fontsize="20") >> api_ec2_1
    codedeploy >> Edge(label="deploy consumer", color="blue", style="dashed", constraint="false", fontsize="20") >> consumer_ec2_1
