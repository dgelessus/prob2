import de.prob.statespace.*

m = api.b_load(dir+"/machines/scheduler.mch")
s = m.getStatespace()
h = new Trace(s)
h1 = h.anyOperation(".*i.*")
assert h1 != h
h2 = h1.anyOperation(".*z.*")
assert h2 == h1
h = h2
h = h.anyOperation("new")
assert h.current.getOp().getName() == "new"
h = h.anyOperation(["new","del"])
assert h.current.getOp().getName() == "new" || h.current.getOp().getName() == "del"
h = h.anyEvent("nr_ready")
assert h.current.getOp().getName() == "nr_ready"

s.animator.cli.shutdown();
"anyOperation and anyEvent on Trace work correctly"