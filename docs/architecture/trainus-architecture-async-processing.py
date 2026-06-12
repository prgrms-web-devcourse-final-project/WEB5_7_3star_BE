from diagrams import Cluster, Diagram, Edge
from diagrams.aws.compute import EC2
from diagrams.aws.network import ALB
from diagrams.onprem.database import PostgreSQL
from diagrams.onprem.inmemory import Redis


graph_attr = {
    "fontsize": "18",
    "pad": "0.6",
    "splines": "polyline",
    "nodesep": "1.0",
    "ranksep": "1.3",
}

node_attr = {
    "fontsize": "19",
    "margin": "0.20",
}

edge_attr = {
    "fontsize": "20",
}

cluster_attr = {
    "fontsize": "22",
}


with Diagram(
    "TrainUs Architecture - Async Processing",
    show=False,
    filename="trainus-architecture-async-processing",
    outformat="png",
    direction="LR",
    graph_attr=graph_attr,
    node_attr=node_attr,
    edge_attr=edge_attr,
):
    client = ALB("Client / ALB")

    with Cluster("API Server", graph_attr=cluster_attr):
        api = EC2("API Server\n(api profile)")

    with Cluster("Redis Core", graph_attr=cluster_attr):
        core = Redis("Redis Core\nstock / duplicate")

    with Cluster("Consumer Server - Admission", graph_attr=cluster_attr):
        admission = EC2("Consumer Server\n(admission dequeue)")
        stream = Redis("Redis Stream\nlesson:apply:stream")

    with Cluster("Consumer Server", graph_attr=cluster_attr):
        consumer = EC2("Consumer Server\n(batch insert)")

    with Cluster("Database", graph_attr=cluster_attr):
        db = PostgreSQL("PostgreSQL / PostGIS")

    client >> Edge(label="apply request", fontsize="20") >> api
    api >> Edge(label="duplicate / stock check", fontsize="20") >> core
    core >> Edge(label="dequeue requestIds", fontsize="20") >> admission
    admission >> Edge(label="SET PROCESSING + XADD", fontsize="20") >> stream
    stream >> Edge(label="XREADGROUP batch", fontsize="20") >> consumer
    consumer >> Edge(label="batch insert", fontsize="20") >> db
    consumer >> Edge(label="XACK / XDEL", dir="back", fontsize="20") >> stream
