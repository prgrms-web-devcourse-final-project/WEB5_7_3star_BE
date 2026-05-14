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


with Diagram(
    "TrainUs Architecture - Async Processing",
    show=False,
    filename="trainus-architecture-async-processing",
    outformat="png",
    direction="LR",
    graph_attr=graph_attr,
):
    client = ALB("Client / ALB")

    with Cluster("API Server"):
        api = EC2("API Server\n(api profile)")

    with Cluster("Redis Core"):
        core = Redis("Redis Core\nstock / duplicate")

    with Cluster("Consumer Server - Admission"):
        admission = EC2("Consumer Server\n(admission dequeue)")
        stream = Redis("Redis Stream\nlesson:apply:stream")

    with Cluster("Consumer Server"):
        consumer = EC2("Consumer Server\n(batch insert)")

    with Cluster("Database"):
        db = PostgreSQL("PostgreSQL / PostGIS")

    client >> Edge(label="apply request") >> api
    api >> Edge(label="duplicate / stock check") >> core
    core >> Edge(label="dequeue requestIds") >> admission
    admission >> Edge(label="SET PROCESSING + XADD") >> stream
    stream >> Edge(label="XREADGROUP batch") >> consumer
    consumer >> Edge(label="batch insert") >> db
    consumer >> Edge(label="XACK / XDEL", dir="back") >> stream
