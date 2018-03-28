//fill in people's details here
const people = [
    {
        name:"Tiark Rompf",
        image:"tiarkrompf.png"
    },
    {
        name:"Fei Wang",
        image: "feiwang.jpg" //images will need to be in website/images/people
    },
    {
        name: "Xilun Wu",
        image: "xilunwu.jpg"
    },
    {
        name: "Gregory Essertel",
        image: "gregoryessertel.jpg"
    },
    {
        name: "James Decker",
        image: "jamesdecker.jpg"
    },
    {
        name: "Guannan Wei",
        image: "guannanwei.jpg"
    },
    {
        name: "Vritant Bhardwaj",
        image: "vritantbhardwaj.jpg"
    }
    //...
];

function populate() {
    const peopleDiv = document.getElementById("people");
    const peopleHTMLString = people.reduce((acc, obj) => acc + getPersonDiv(obj.name, obj.image), "");
    peopleDiv.innerHTML = peopleHTMLString;
}

function getPersonDiv(name, img) {
    return  `<div class="person flex-center flex-column">
                <img class="person-img" src="./website/images/people/${img}">
                <div class="person-name">${name}</div>
            </div>`
}

window.onload = () => {
    populate();
}
