Vagrant.configure("2") do |config|
  config.vm.define "consul-0" do |node|
      node.vm.box = "ubuntu/bionic64"
      node.vm.hostname = "consul-0"
      serverIp = "192.168.99.100"
      node.vm.network "private_network", ip: serverIp
      node.vm.provision "shell", path: "provision.sh", args: "#{serverIp}"
      node.vm.provision "shell", inline: "service consul start"
  end

  config.vm.define "consul-1" do |node|
      node.vm.box = "ubuntu/bionic64"
      node.vm.hostname = "consul-1"
      serverIp = "192.168.99.101"
      node.vm.network "private_network", ip: serverIp
      node.vm.provision "shell", path: "provision.sh", args: "#{serverIp}"
      node.vm.provision "shell", inline: "service consul start"
      node.vm.provision "shell", inline: "consul join -http-addr=http://192.168.99.101:8500 192.168.99.100"
  end

  config.vm.define "consul-2" do |node|
      node.vm.box = "ubuntu/bionic64"
      node.vm.hostname = "consul-2"
      serverIp = "192.168.99.102"
      node.vm.network "private_network", ip: serverIp
      node.vm.provision "shell", path: "provision.sh", args: "#{serverIp}"
      node.vm.provision "shell", inline: "service consul start"
      node.vm.provision "shell", inline: "consul join -http-addr=http://192.168.99.102:8500 192.168.99.100"
  end
end